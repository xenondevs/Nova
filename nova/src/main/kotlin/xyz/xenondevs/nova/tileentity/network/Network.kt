package xyz.xenondevs.nova.tileentity.network

import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.tileentity.network.node.NetworkEndPoint
import xyz.xenondevs.nova.tileentity.network.node.NetworkNode
import xyz.xenondevs.nova.tileentity.network.node.NetworkNodeConnection
import xyz.xenondevs.nova.tileentity.network.type.NetworkType
import xyz.xenondevs.nova.world.BlockPos
import java.util.*

/**
 * The data of a network, containing all [NetworkNodes][NetworkNode] and their connections,
 * as well as the [type] and [uuid] of the network.
 */
interface NetworkData<T : Network<T>> {
    
    /**
     * The type of this [NetworkData].
     */
    val type: NetworkType<T>
    
    /**
     * The unique identifier of this [NetworkData].
     */
    val uuid: UUID
    
    /**
     * The [NetworkNodes][NetworkNode] within this [NetworkData] and the [BlockFaces][BlockFace]
     * through which [NetworkEndPoints][NetworkEndPoint] connect to it.
     */
    val nodes: Map<BlockPos, NetworkNodeConnection>
    
}

internal class ImmutableNetworkData<T : Network<T>>(
    override val type: NetworkType<T>,
    override val uuid: UUID,
    override val nodes: Map<BlockPos, NetworkNodeConnection>
) : NetworkData<T>

/**
 * A network is an immutable data structure that is created from a [NetworkData].
 *
 * It contains [NetworkNodes][NetworkNode] and handles the ticking logic.
 */
interface Network<S : Network<S>> : NetworkData<S>