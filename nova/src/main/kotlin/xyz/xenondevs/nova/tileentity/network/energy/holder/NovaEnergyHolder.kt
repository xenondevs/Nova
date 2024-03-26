package xyz.xenondevs.nova.tileentity.network.energy.holder

import xyz.xenondevs.nova.util.serverTick
import kotlin.reflect.KProperty

// TODO

//sealed class NovaEnergyHolder(
//    final override val endPoint: NetworkedTileEntity,
//    maxEnergy: Provider<Long>,
//    lazyDefaultConfig: () -> EnumMap<BlockFace, NetworkConnectionType>
//) : EnergyHolder {
//    
//    val updateHandlers = ArrayList<() -> Unit>()
//    
//    override val connectionConfig: MutableMap<BlockFace, NetworkConnectionType> =
//        endPoint.retrieveData("energyConfig") { lazyDefaultConfig() }
//    
//    val maxEnergy: Long by maxEnergy
//    
//    override var energy: Long = endPoint.retrieveData("energy") { 0L }
//        set(value) {
//            val capped = max(min(value, maxEnergy), 0)
//            if (field != capped) {
//                val energyDelta = capped - field
//                if (energyDelta > 0) _energyPlus.add(energyDelta)
//                else _energyMinus.add(-energyDelta)
//                
//                field = capped
//                callUpdateHandlers()
//            }
//        }
//    
//    override val requestedEnergy: Long
//        get() = maxEnergy - energy
//    
//    private val _energyMinus = TickedLong()
//    private val _energyPlus = TickedLong()
//    
//    val energyMinus by _energyMinus
//    val energyPlus by _energyPlus
//    
//    override fun saveData() {
//        endPoint.storeData("energy", energy, true)
//        endPoint.storeData("energyConfig", connectionConfig)
//    }
//    
//    override fun reload() {
//        maxEnergy = calculateMaxEnergy()
//        
//        if (energy > maxEnergy) {
//            energy = maxEnergy // this will call the update handlers as well
//        } else callUpdateHandlers()
//    }
//    
//    private fun callUpdateHandlers() =
//        updateHandlers.forEach { it() }
//    
//}

private class TickedLong {
    
    private var lastUpdated: Int = 0
    private var value: Long = 0
    private var prevValue: Long = 0
    
    fun get(): Long {
        if (lastUpdated != serverTick) {
            prevValue = value
            value = 0
            lastUpdated = serverTick
        }
        
        return if (value == 0L) prevValue else value
    }
    
    operator fun getValue(thisRef: Any, property: KProperty<*>): Long = get()
    
    fun set(value: Long) {
        if (lastUpdated != serverTick) {
            prevValue = value
            lastUpdated = serverTick
        }
        
        this.value = value
    }
    
    fun add(value: Long) {
        if (lastUpdated != serverTick) {
            prevValue = value
            this.value = 0
            lastUpdated = serverTick
        }
        
        this.value += value
    }
    
}