package xyz.xenondevs.nova.world.block.tileentity.network.node

import xyz.xenondevs.nova.util.CubeFaceSet
import xyz.xenondevs.nova.world.block.tileentity.network.Network

/**
 * Represents a connection of a [NetworkNode] to a [Network].
 *
 * @property node The network node that is connected.
 * @property faces The faces through which [node] connects to the network.
 */
data class NetworkNodeConnection(
    val node: NetworkNode,
    val faces: CubeFaceSet = CubeFaceSet.NONE
)