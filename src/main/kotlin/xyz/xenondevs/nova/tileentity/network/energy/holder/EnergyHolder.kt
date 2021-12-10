package xyz.xenondevs.nova.tileentity.network.energy.holder

import de.studiocode.invui.item.ItemBuilder
import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.data.config.PermanentStorage.retrieve
import xyz.xenondevs.nova.data.config.PermanentStorage.retrieveOrNull
import xyz.xenondevs.nova.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.tileentity.network.EndPointDataHolder
import xyz.xenondevs.nova.tileentity.network.energy.EnergyConnectionType
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeHolder
import xyz.xenondevs.nova.util.PrefixUtils
import xyz.xenondevs.nova.util.serverTick
import kotlin.math.max
import kotlin.math.min
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

sealed class EnergyHolder(
    final override val endPoint: NetworkedTileEntity,
    private val defaultMaxEnergy: Long,
    protected val upgradeHolder: UpgradeHolder?,
    lazyDefaultConfig: () -> MutableMap<BlockFace, EnergyConnectionType>
) : EndPointDataHolder {
    
    val updateHandlers = ArrayList<() -> Unit>()
    
    val energyConfig: MutableMap<BlockFace, EnergyConnectionType> =
        endPoint.retrieveDoubleEnumMap("energyConfig") { lazyDefaultConfig() }
    
    override val allowedFaces: Set<BlockFace>
        get() = energyConfig.mapNotNullTo(HashSet()) { if (it.value == EnergyConnectionType.NONE) null else it.key }
    
    var maxEnergy = (defaultMaxEnergy * (upgradeHolder?.getEnergyModifier() ?: 1.0)).toLong()
    
    open var energy: Long =
        endPoint.retrieveOrNull("energy64")
            ?: retrieveOrNull<Int>("energy")?.toLong()
            ?: retrieve("energy64") { 0L }
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
    
    open val requestedEnergy: Long
        get() = maxEnergy - energy
    
    var energyMinus by TickedLong()
    var energyPlus by TickedLong()
    
    init {
        upgradeHolder?.upgradeUpdateHandlers?.add(::handleUpgradesUpdate)
    }
    
    override fun saveData() {
        endPoint.storeData("energy64", energy, true)
        endPoint.storeEnumMap("energyConfig", energyConfig)
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
            builder.addLoreLines("§7" + PrefixUtils.getEnergyString(energy))
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