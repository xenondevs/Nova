package xyz.xenondevs.nova.tileentity.network.type.energy.holder

import org.bukkit.block.BlockFace
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.cbf.provider.entry
import xyz.xenondevs.commons.collections.toEnumMap
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.mutable.MutableProvider
import xyz.xenondevs.commons.provider.mutable.defaultsToLazily
import xyz.xenondevs.commons.provider.mutable.orElse
import xyz.xenondevs.nova.data.serialization.DataHolder
import xyz.xenondevs.nova.tileentity.network.type.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.type.energy.EnergyNetwork
import xyz.xenondevs.nova.util.ResettingLongProvider
import kotlin.math.max
import kotlin.math.min

/**
 * The default [EnergyHolder] implementation.
 *
 * @param compound the [Compound] for data storage and retrieval
 * @param maxEnergyProvider the maximum amount of energy this [EnergyHolder] can store
 * @param allowedConnectionType determines whether energy can be inserted, extracted, or both
 * @param defaultConnectionConfig the default ([BlockFace], [NetworkConnectionType]) to be used if no configuration is stored
 */
class DefaultEnergyHolder(
    compound: Provider<Compound>,
    val maxEnergyProvider: Provider<Long>,
    override val allowedConnectionType: NetworkConnectionType,
    defaultConnectionConfig: () -> Map<BlockFace, NetworkConnectionType>
) : EnergyHolder {
    
    private val _energyProvider: MutableProvider<Long> = compound.entry<Long>("energy").orElse(0L)
    private val _energyMinusProvider = ResettingLongProvider(EnergyNetwork.TICK_DELAY_PROVIDER)
    private val _energyPlusProvider = ResettingLongProvider(EnergyNetwork.TICK_DELAY_PROVIDER)
    
    override val connectionConfig: MutableMap<BlockFace, NetworkConnectionType>
        by compound.entry<MutableMap<BlockFace, NetworkConnectionType>>("connectionConfig")
            .defaultsToLazily { defaultConnectionConfig().toEnumMap() }
    
    /**
     * The maximum amount of energy this [EnergyHolder] can store.
     */
    val maxEnergy: Long by maxEnergyProvider
    
    /**
     * A [Provider] for the current energy amount.
     */
    val energyProvider: Provider<Long> get() = _energyProvider
    
    /**
     * A [Provider] that shows the amount of energy that was extracted during the last energy network tick.
     */
    val energyMinusProvider: Provider<Long> = _energyMinusProvider
    
    /**
     * A [Provider] that shows the amount of energy that was inserted during the last energy network tick.
     */
    val energyPlusProvider: Provider<Long> = _energyPlusProvider
    
    /**
     * The amount of energy that was extracted during the last energy network tick.
     */
    val energyMinus: Long by _energyMinusProvider
    
    /**
     * The amount of energy that was inserted during the last energy network tick.
     */
    val energyPlus: Long by _energyPlusProvider
    
    override var energy: Long
        get() = _energyProvider.get()
        set(value) {
            val capped = max(min(value, maxEnergy), 0)
            if (_energyProvider.get() != capped) {
                val energyDelta = capped - _energyProvider.get()
                if (energyDelta > 0) {
                    _energyPlusProvider.add(energyDelta)
                } else {
                    _energyMinusProvider.add(-energyDelta)
                }
                
                _energyProvider.set(capped)
            }
        }
    
    override val requestedEnergy: Long
        get() = maxEnergy - energy
    
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