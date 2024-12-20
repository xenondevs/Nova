package xyz.xenondevs.nova.world.block.tileentity.network.type.energy.holder

import org.bukkit.block.BlockFace
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.cbf.entry
import xyz.xenondevs.commons.collections.toEnumMap
import xyz.xenondevs.commons.collections.toEnumSet
import xyz.xenondevs.commons.provider.MutableProvider
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.observed
import xyz.xenondevs.commons.provider.orElseNew
import xyz.xenondevs.nova.serialization.DataHolder
import xyz.xenondevs.nova.util.TickResettingLong
import xyz.xenondevs.nova.world.block.tileentity.network.type.NetworkConnectionType
import kotlin.math.max
import kotlin.math.min

/**
 * The default [EnergyHolder] implementation.
 *
 * @param compound the [Compound] for data storage and retrieval
 * @param energy the [Provider] for the current energy amount
 * @param maxEnergyProvider the maximum amount of energy this [EnergyHolder] can store
 * @param allowedConnectionType determines whether energy can be inserted, extracted, or both
 * @param defaultConnectionConfig the default ([BlockFace], [NetworkConnectionType]) to be used if no configuration is stored
 */
class DefaultEnergyHolder(
    compound: Provider<Compound>,
    energy: MutableProvider<Long>,
    val maxEnergyProvider: Provider<Long>,
    override val allowedConnectionType: NetworkConnectionType,
    blockedFaces: Set<BlockFace>,
    defaultConnectionConfig: () -> Map<BlockFace, NetworkConnectionType>
) : EnergyHolder {
    
    private val _energyProvider: MutableProvider<Long> = energy
    private val _energyMinus = TickResettingLong()
    private val _energyPlus = TickResettingLong()
    
    override val blockedFaces = blockedFaces.toEnumSet()
    override val connectionConfig: MutableMap<BlockFace, NetworkConnectionType>
        by compound.entry<MutableMap<BlockFace, NetworkConnectionType>>("connectionConfig")
            .orElseNew {
                val map = defaultConnectionConfig().toEnumMap()
                for (face in blockedFaces)
                    map[face] = NetworkConnectionType.NONE
                map
            }
            .observed()
    
    /**
     * The maximum amount of energy this [EnergyHolder] can store.
     */
    override val maxEnergy: Long by maxEnergyProvider
    
    /**
     * A [Provider] for the current energy amount.
     */
    val energyProvider: Provider<Long> get() = _energyProvider
    
    /**
     * The amount of energy that was extracted during the last server tick.
     */
    val energyMinus: Long by _energyMinus
    
    /**
     * The amount of energy that was inserted during the last server tick.
     */
    val energyPlus: Long by _energyPlus
    
    override var energy: Long
        get() = _energyProvider.get()
        set(value) {
            val capped = max(min(value, maxEnergy), 0)
            if (_energyProvider.get() != capped) {
                val energyDelta = capped - _energyProvider.get()
                if (energyDelta > 0) {
                    _energyPlus.add(energyDelta)
                } else {
                    _energyMinus.add(-energyDelta)
                }
                
                _energyProvider.set(capped)
            }
        }
    
    internal companion object {
        
        fun tryConvertLegacy(dataHolder: DataHolder): Compound? {
            val connectionConfig: MutableMap<BlockFace, NetworkConnectionType>? = dataHolder.retrieveDataOrNull("energyConfig")
                ?: return null
            
            dataHolder.removeData("energyConfig")
            
            val compound = Compound() // new format
            if (connectionConfig != null)
                compound["connectionConfig"] = connectionConfig
            
            return compound
        }
        
    }
    
}