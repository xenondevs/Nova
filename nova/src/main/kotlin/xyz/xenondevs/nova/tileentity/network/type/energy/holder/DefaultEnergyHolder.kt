package xyz.xenondevs.nova.tileentity.network.type.energy.holder

import org.bukkit.block.BlockFace
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.cbf.provider.entry
import xyz.xenondevs.commons.collections.toEnumMap
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.mutable.defaultsToLazily
import xyz.xenondevs.commons.provider.mutable.orElse
import xyz.xenondevs.nova.data.serialization.DataHolder
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
    compound: Provider<Compound>,
    maxEnergy: Provider<Long>,
    override val allowedConnectionType: NetworkConnectionType,
    defaultConnectionConfig: () -> Map<BlockFace, NetworkConnectionType>
) : EnergyHolder {
    
    override val connectionConfig: MutableMap<BlockFace, NetworkConnectionType>
        by compound.entry<MutableMap<BlockFace, NetworkConnectionType>>("connectionConfig")
            .defaultsToLazily { defaultConnectionConfig().toEnumMap() }
    
    /**
     * The maximum amount of energy this [EnergyHolder] can store.
     */
    val maxEnergy: Long by maxEnergy
    
    private var _energy by compound.entry<Long>("energy").orElse(0L)
    override var energy: Long
        get() = _energy
        set(value) {
            val capped = max(min(value, maxEnergy), 0)
            if (_energy != capped) {
                val energyDelta = capped - _energy
                if (energyDelta > 0) _energyPlus.add(energyDelta)
                else _energyMinus.add(-energyDelta)
                
                _energy = capped
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
    
    init {
        maxEnergy.addUpdateHandler { callUpdateHandlers() }
    }
    
    private fun callUpdateHandlers() =
        updateHandlers.forEach { it() }
    
    internal companion object {
        
        fun tryConvertLegacy(dataHolder: DataHolder): Compound? {
            val connectionConfig: MutableMap<BlockFace, NetworkConnectionType>? = 
                dataHolder.retrieveDataOrNull("energyConfig")
            val energy: Long? = 
                dataHolder.retrieveDataOrNull("energy")
            
            if (connectionConfig == null &&
                energy == null
            ) return null
            
            dataHolder.removeData("energyConfig")
            dataHolder.removeData("energy")
            
            val compound = Compound() // new format
            if (connectionConfig != null)
                compound["connectionConfig"] = connectionConfig
            if (energy != null)
                compound["energy"] = energy
            
            return compound
        }
        
    }
    
}