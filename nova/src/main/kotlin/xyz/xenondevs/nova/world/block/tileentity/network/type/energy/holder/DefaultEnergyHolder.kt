package xyz.xenondevs.nova.world.block.tileentity.network.type.energy.holder

import org.bukkit.block.BlockFace
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.cbf.entry
import xyz.xenondevs.commons.collections.toEnumMap
import xyz.xenondevs.commons.collections.toEnumSet
import xyz.xenondevs.commons.provider.MutableProvider
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.mutableProvider
import xyz.xenondevs.commons.provider.observed
import xyz.xenondevs.commons.provider.orElseNew
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
    private val _energyMinusProvider = mutableProvider(0L)
    private val _energyPlusProvider = mutableProvider(0L)
    private var activeEnergyMinus = 0L
    private var activeEnergyPlus = 0L
    
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
     * A [Provider] containing the amount of energy that was extracted between the second-to-last and last energy network tick.
     * For visualization, this value should be normalized by dividing it by the number of game ticks between energy network ticks.
     */
    val energyMinusProvider: Provider<Long>
        get() = _energyMinusProvider
    
    /**
     * A [Provider] containing the amount of energy that was inserted between the second-to-last and last energy network tick.
     * For visualization, this value should be normalized by dividing it by the number of game ticks between energy network ticks.
     */
    val energyPlusProvider: Provider<Long>
        get() = _energyPlusProvider
    
    /**
     * The amount of energy that was extracted between the second-to-last and last energy network tick.
     * For visualization, this value should be normalized by dividing it by the number of game ticks between energy network ticks.
     */
    val energyMinus: Long by energyMinusProvider
    
    /**
     * The amount of energy that was inserted between the second-to-last and last energy network tick.
     * For visualization, this value should be normalized by dividing it by the number of game ticks between energy network ticks.
     */
    val energyPlus: Long by energyPlusProvider
    
    override var energy: Long
        get() = _energyProvider.get()
        set(value) {
            val capped = max(min(value, maxEnergy), 0)
            if (_energyProvider.get() != capped) {
                val energyDelta = capped - _energyProvider.get()
                if (energyDelta > 0) {
                    activeEnergyPlus += energyDelta
                } else {
                    activeEnergyMinus -= energyDelta
                }
                
                _energyProvider.set(capped)
            }
        }
    
    /**
     * Called by the network group ticking the energy network of this holder to flush the accumulated
     * energy plus and minus values to [energyPlus] and [energyMinus].
     */
    fun postTick() {
        _energyMinusProvider.set(activeEnergyMinus)
        _energyPlusProvider.set(activeEnergyPlus)
        activeEnergyMinus = 0L
        activeEnergyPlus = 0L
    }
    
}