package xyz.xenondevs.nova.tileentity.network.type.fluid.holder

import org.bukkit.block.BlockFace
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.commons.collections.enumMap
import xyz.xenondevs.commons.collections.toEnumMap
import xyz.xenondevs.nova.tileentity.network.type.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.type.fluid.container.NetworkedFluidContainer
import xyz.xenondevs.nova.util.CUBE_FACES
import java.util.*

/**
 * The default [FluidHolder] implementation.
 * 
 * @param compound the [Compound] for data storage and retrieval
 * @param containers all available [NetworkedFluidContainers][NetworkedFluidContainer] and their allowed [NetworkConnectionType]
 * @param defaultContainerConfig the default ([BlockFace], [NetworkedFluidContainer]) to be used if no configuration is stored
 * @param defaultConnectionConfig the default ([BlockFace], [NetworkConnectionType]) to be used if no configuration is stored
 */
class DefaultFluidHolder(
    override val compound: Compound,
    override val containers: Map<NetworkedFluidContainer, NetworkConnectionType>,
    defaultContainerConfig: () -> Map<BlockFace, NetworkedFluidContainer>,
    defaultConnectionConfig: () -> Map<BlockFace, NetworkConnectionType>
) : FluidHolder {
    
    init {
        if (containers.isEmpty())
            throw IllegalArgumentException("availableContainers must not be empty")
    }
    
    private val uuidToContainer: Map<UUID, NetworkedFluidContainer> =
        containers.keys.associateByTo(HashMap()) { it.uuid }
    
    override val containerConfig: MutableMap<BlockFace, NetworkedFluidContainer> =
        compound.get<Map<BlockFace, UUID>>("containerConfig")
            ?.mapValuesTo(enumMap()) { uuidToContainer[it.value] }
            ?: defaultContainerConfig().toEnumMap()
    
    override val connectionConfig: MutableMap<BlockFace, NetworkConnectionType> =
        compound["connectionConfig"] ?: defaultConnectionConfig().toEnumMap()
    
    override val channels: MutableMap<BlockFace, Int> =
        compound["channels"] ?: DEFAULT_CHANNEL_CONFIG()
    
    override val insertPriorities: MutableMap<BlockFace, Int> =
        compound["insertPriorities"] ?: DEFAULT_PRIORITIES()
    
    override val extractPriorities: MutableMap<BlockFace, Int> =
        compound["extractPriorities"] ?: DEFAULT_PRIORITIES()
    
    override fun saveData() {
        compound["containerConfig"] = containerConfig.mapValuesTo(enumMap()) { it.value.uuid }
        compound["connectionConfig"] = connectionConfig
        compound["channels"] = channels
        compound["insertPriorities"] = insertPriorities
        compound["extractPriorities"] = extractPriorities
    }
    
    internal companion object {
        val DEFAULT_CONNECTION_CONFIG = { CUBE_FACES.associateWithTo(enumMap()) { NetworkConnectionType.NONE } }
        val DEFAULT_CHANNEL_CONFIG = { CUBE_FACES.associateWithTo(enumMap()) { 0 } }
        val DEFAULT_PRIORITIES = { CUBE_FACES.associateWithTo(enumMap()) { 50 } }
    }
    
}