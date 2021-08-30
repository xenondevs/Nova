package xyz.xenondevs.nova.tileentity.network.energy.holder

import de.studiocode.invui.item.ItemBuilder
import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.tileentity.network.EndPointDataHolder
import xyz.xenondevs.nova.tileentity.network.NetworkType
import xyz.xenondevs.nova.tileentity.network.energy.EnergyConnectionType
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeHolder
import xyz.xenondevs.nova.util.EnergyUtils
import xyz.xenondevs.nova.util.serverTick
import java.lang.Integer.max
import java.lang.Integer.min
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

sealed class EnergyHolder(
    final override val endPoint: NetworkedTileEntity,
    private val defaultMaxEnergy: Int,
    protected val upgradeHolder: UpgradeHolder?,
    lazyDefaultConfig: () -> MutableMap<BlockFace, EnergyConnectionType>
) : EndPointDataHolder {
    
    val updateHandlers = ArrayList<() -> Unit>()
    
    val energyConfig: MutableMap<BlockFace, EnergyConnectionType> =
        endPoint.retrieveDoubleEnumMap("energyConfig") { lazyDefaultConfig() }
    
    override val allowedFaces: Set<BlockFace>
        get() = energyConfig.mapNotNullTo(HashSet()) { if (it.value == EnergyConnectionType.NONE) null else it.key }
    
    var maxEnergy = (defaultMaxEnergy * (upgradeHolder?.getEnergyModifier() ?: 1.0)).toInt()
    
    open var energy: Int = endPoint.retrieveData("energy") { 0 }
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
    
    open val requestedEnergy: Int
        get() = maxEnergy - energy
    
    var energyMinus by TickedInt()
    var energyPlus by TickedInt()
    
    init {
        upgradeHolder?.upgradeUpdateHandlers?.add(::handleUpgradesUpdate)
    }
    
    override fun saveData() {
        endPoint.storeData("energy", energy, true)
        endPoint.storeEnumMap("energyConfig", energyConfig)
    }
    
    protected open fun handleUpgradesUpdate() {
        maxEnergy = calculateMaxEnergy()
        
        if (energy > maxEnergy) {
            energy = maxEnergy // this will call the update handlers as well
        } else callUpdateHandlers()
    }
    
    private fun calculateMaxEnergy(): Int =
        (defaultMaxEnergy * (upgradeHolder?.getEnergyModifier() ?: 1.0)).toInt()
    
    private fun callUpdateHandlers() =
        updateHandlers.forEach { it() }
    
    companion object {
        
        fun createItemBuilder(material: NovaMaterial, tileEntity: TileEntity?): ItemBuilder {
            val builder = material.createBasicItemBuilder()
            val energy = tileEntity?.let { ((tileEntity as NetworkedTileEntity).holders[NetworkType.ENERGY] as EnergyHolder).energy }
                ?: 0
            builder.addLoreLines("§7" + EnergyUtils.getEnergyString(energy))
            return builder
        }
        
    }
    
}

private class TickedInt : ReadWriteProperty<Any, Int> {
    
    var lastUpdated: Int = 0
    var value: Int = 0
    
    override fun getValue(thisRef: Any, property: KProperty<*>): Int {
        if (lastUpdated != serverTick) {
            value = 0
            lastUpdated = serverTick
        }
        
        return value
    }
    
    override fun setValue(thisRef: Any, property: KProperty<*>, value: Int) {
        this.value = value
    }
    
}