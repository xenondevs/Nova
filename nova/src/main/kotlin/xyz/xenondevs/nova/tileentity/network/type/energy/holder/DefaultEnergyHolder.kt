package xyz.xenondevs.nova.tileentity.network.type.energy.holder

import org.bukkit.block.BlockFace
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.commons.collections.toEnumMap
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.nova.tileentity.network.type.NetworkConnectionType
import xyz.xenondevs.nova.util.TickedLong
import kotlin.math.max
import kotlin.math.min

/**
 * The default [EnergyHolder] implementation.
 * 
 * @param compound the [Compound] for data storage and retrieval
 * @param maxEnergy the maximum amount of energy this [EnergyHolder] can store
 * @param allowedConnectionType determines whether energy can be inserted, extracted, or both
 * @param defaultConnectionConfig the default ([BlockFace], [NetworkConnectionType]) to be used if no configuration is stored
 */
class DefaultEnergyHolder(
    override val compound: Compound,
    maxEnergy: Provider<Long>,
    override val allowedConnectionType: NetworkConnectionType,
    defaultConnectionConfig: () -> Map<BlockFace, NetworkConnectionType>
) : EnergyHolder {
    
    override val connectionConfig: MutableMap<BlockFace, NetworkConnectionType> =
        compound["connectionConfig"] ?: defaultConnectionConfig().toEnumMap()
    
    /**
     * The maximum amount of energy this [EnergyHolder] can store.
     */
    val maxEnergy: Long by maxEnergy
    
    override var energy: Long = compound["energy"] ?: 0L
        set(value) {
            val capped = max(min(value, maxEnergy), 0)
            if (field != capped) {
                val energyDelta = capped - field
                if (energyDelta > 0) _energyPlus.add(energyDelta)
                else _energyMinus.add(-energyDelta)
                
                field = capped
                callUpdateHandlers()
            }
        }
    
    override val requestedEnergy: Long
        get() = maxEnergy - energy
    
    private val _energyMinus = TickedLong()
    private val _energyPlus = TickedLong()
    
    // TODO: energyPlus and energyMinus won't work properly if EnergyNetwork tick delay > 1
    /**
     * The amount of energy that was removed from this [EnergyHolder] since the last server tick.
     */
    val energyMinus by _energyMinus
    
    /**
     * The amount of energy that was added to this [EnergyHolder] since the last server tick.
     */
    val energyPlus by _energyPlus
    
    /**
     * A list of handlers that are called when the energy of this [EnergyHolder] changes.
     */
    val updateHandlers = ArrayList<() -> Unit>()
    
    override fun saveData() {
        compound["energy"] = energy
        compound["connectionConfig"] = connectionConfig
    }
    
    private fun callUpdateHandlers() =
        updateHandlers.forEach { it() }
    
}