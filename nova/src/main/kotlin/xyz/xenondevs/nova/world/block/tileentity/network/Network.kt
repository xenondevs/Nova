package xyz.xenondevs.nova.world.block.tileentity.network

import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkEndPoint
import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkNode
import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkNodeConnection
import xyz.xenondevs.nova.world.block.tileentity.network.type.NetworkType
import java.util.*
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator

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
interface Network<S : Network<S>> : NetworkData<S> {
    
    /**
     * Checks whether this [Network] is valid, i.e. if it is allowed to tick.
     */
    fun isValid(): Boolean {
        for ((_, con) in nodes) {
            if (!con.node.isValid)
                return false
        }
        
        return true
    }
    
}