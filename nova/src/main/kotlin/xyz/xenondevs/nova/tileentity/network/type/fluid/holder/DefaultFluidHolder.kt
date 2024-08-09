package xyz.xenondevs.nova.tileentity.network.type.fluid.holder

import org.bukkit.block.BlockFace
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.nova.data.serialization.DataHolder
import xyz.xenondevs.nova.tileentity.network.node.DefaultContainerEndPointDataHolder
import xyz.xenondevs.nova.tileentity.network.type.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.type.fluid.container.NetworkedFluidContainer
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
    compound: Provider<Compound>,
    containers: Map<NetworkedFluidContainer, NetworkConnectionType>,
    blockedFaces: Set<BlockFace>,
    defaultContainerConfig: () -> Map<BlockFace, NetworkedFluidContainer>,
    defaultConnectionConfig: (() -> Map<BlockFace, NetworkConnectionType>)?
) : DefaultContainerEndPointDataHolder<NetworkedFluidContainer>(
    compound,
    containers,
    blockedFaces,
    defaultContainerConfig,
    defaultConnectionConfig
), FluidHolder {
    
    override val uuidToContainer: Map<UUID, NetworkedFluidContainer> =
        containers.keys.associateByTo(HashMap()) { it.uuid }
    
    internal companion object {
        
        fun tryConvertLegacy(dataHolder: DataHolder): Compound? {
            val containerConfig: MutableMap<BlockFace, UUID>? =
                dataHolder.retrieveDataOrNull("fluidContainerConfig")
            val connectionConfig: MutableMap<BlockFace, NetworkConnectionType>? =
                dataHolder.retrieveDataOrNull("fluidConnectionConfig")
            val insertPriorities: MutableMap<BlockFace, Int>? =
                dataHolder.retrieveDataOrNull("fluidInsertPriorities")
            val extractPriorities: MutableMap<BlockFace, Int>? =
                dataHolder.retrieveDataOrNull("fluidExtractPriorities")
            val channels: MutableMap<BlockFace, Int>? =
                dataHolder.retrieveDataOrNull("fluidChannels")
            
            if (containerConfig == null &&
                connectionConfig == null &&
                insertPriorities == null &&
                extractPriorities == null &&
                channels == null
            ) return null
            
            dataHolder.removeData("fluidContainerConfig")
            dataHolder.removeData("fluidConnectionConfig")
            dataHolder.removeData("fluidInsertPriorities")
            dataHolder.removeData("fluidExtractPriorities")
            dataHolder.removeData("fluidChannels")
            
            val compound = Compound() // new format
            if (containerConfig != null)
                compound["containerConfig"] = containerConfig
            if (connectionConfig != null)
                compound["connectionConfig"] = connectionConfig
            if (insertPriorities != null)
                compound["insertPriorities"] = insertPriorities
            if (extractPriorities != null)
                compound["extractPriorities"] = extractPriorities
            if (channels != null)
                compound["channels"] = channels
            
            return compound
        }
        
    }
    
}