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
interface NetworkData {
    
    /**
     * The type of this [NetworkData].
     */
    val type: NetworkType
    
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

internal class ImmutableNetworkData(
    override val type: NetworkType,
    override val uuid: UUID,
    override val nodes: Map<BlockPos, NetworkNodeConnection>
) : NetworkData
 
/**
 * A network is an immutable data structure that is created from a [NetworkData].
 * 
 * It contains [NetworkNodes][NetworkNode] and how they're connected to this network and
 * handles the ticking logic.
 */
interface Network : NetworkData {
    
    /**
     * Called every [NetworkType.tickDelay] ticks.
     * 
     * Independent networks are ticked in parallel!
     * Because of that, this function may not interact with any world state outside of the blocks that are in this network.
     * This includes not causing block updates and not firing bukkit events.
     */
    fun handleTick()

}
