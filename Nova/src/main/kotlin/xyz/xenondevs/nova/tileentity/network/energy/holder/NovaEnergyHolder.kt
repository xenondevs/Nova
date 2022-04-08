package xyz.xenondevs.nova.tileentity.network.energy.holder

import de.studiocode.invui.item.builder.ItemBuilder
import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeHolder
import xyz.xenondevs.nova.util.NumberFormatUtils
import xyz.xenondevs.nova.util.serverTick
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

sealed class NovaEnergyHolder(
    final override val endPoint: NetworkedTileEntity,
    private val defaultMaxEnergy: Long,
    protected val upgradeHolder: UpgradeHolder?,
    lazyDefaultConfig: () -> EnumMap<BlockFace, NetworkConnectionType>
) : EnergyHolder {
    
    val updateHandlers = ArrayList<() -> Unit>()
    
    override val connectionConfig: MutableMap<BlockFace, NetworkConnectionType> =
        endPoint.retrieveEnumMap("energyConfig") { lazyDefaultConfig() }
    
    var maxEnergy = (defaultMaxEnergy * (upgradeHolder?.getEnergyModifier() ?: 1.0)).toLong()
    
    override var energy: Long = endPoint.retrieveData("energy") { 0L }
        set(value) {
            val capped = max(min(value, maxEnergy), 0)
            if (field != capped) {
                val energyDelta = capped - field
                if (energyDelta > 0) energyPlus += energyDelta
                else energyMinus -= energyDelta
                
                field = capped
                callUpdateHandlers()
            }
        }
    
    override val requestedEnergy: Long
        get() = maxEnergy - energy
    
    var energyMinus by TickedLong()
    var energyPlus by TickedLong()
    
    init {
        upgradeHolder?.upgradeUpdateHandlers?.add(::handleUpgradesUpdate)
    }
    
    override fun saveData() {
        endPoint.storeData("energy", energy, true)
        endPoint.storeEnumMap("energyConfig", connectionConfig)
    }
    
    protected open fun handleUpgradesUpdate() {
        maxEnergy = calculateMaxEnergy()
        
        if (energy > maxEnergy) {
            energy = maxEnergy // this will call the update handlers as well
        } else callUpdateHandlers()
    }
    
    private fun calculateMaxEnergy(): Long =
        (defaultMaxEnergy * (upgradeHolder?.getEnergyModifier() ?: 1.0)).toLong()
    
    private fun callUpdateHandlers() =
        updateHandlers.forEach { it() }
    
    companion object {
        
        fun modifyItemBuilder(builder: ItemBuilder, tileEntity: TileEntity?): ItemBuilder {
            val energy = tileEntity
                ?.let { (tileEntity as NetworkedTileEntity).energyHolder.energy }
                ?: 0
            builder.addLoreLines("§7" + NumberFormatUtils.getEnergyString(energy))
            return builder
        }
        
    }
    
}

private class TickedLong : ReadWriteProperty<Any, Long> {
    
    var lastUpdated: Int = 0
    var value: Long = 0
    
    override fun getValue(thisRef: Any, property: KProperty<*>): Long {
        if (lastUpdated != serverTick) {
            value = 0
            lastUpdated = serverTick
        }
        
        return value
    }
    
    override fun setValue(thisRef: Any, property: KProperty<*>, value: Long) {
        this.value = value
    }
    
}