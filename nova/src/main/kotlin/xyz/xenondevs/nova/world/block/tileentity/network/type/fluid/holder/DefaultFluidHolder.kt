package xyz.xenondevs.nova.world.block.tileentity.network.type.fluid.holder

import xyz.xenondevs.nova.world.*

import org.bukkit.block.BlockFace
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.nova.util.CubeFaceMap
import xyz.xenondevs.nova.util.CubeFaceSet
import xyz.xenondevs.nova.world.block.tileentity.network.node.DefaultContainerEndPointDataHolder
import xyz.xenondevs.nova.world.block.tileentity.network.type.NetworkConnectionType
import xyz.xenondevs.nova.world.block.tileentity.network.type.fluid.container.NetworkedFluidContainer
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
    blockedFaces: CubeFaceSet,
    defaultContainerConfig: CubeFaceMap<NetworkedFluidContainer?>,
    defaultConnectionConfig: CubeFaceMap<NetworkConnectionType>?
) : DefaultContainerEndPointDataHolder<NetworkedFluidContainer>(
    compound,
    containers,
    blockedFaces,
    defaultContainerConfig,
    defaultConnectionConfig
), FluidHolder {
    
    override val uuidToContainer: Map<UUID, NetworkedFluidContainer> =
        containers.keys.associateByTo(HashMap()) { it.uuid }
    
}
