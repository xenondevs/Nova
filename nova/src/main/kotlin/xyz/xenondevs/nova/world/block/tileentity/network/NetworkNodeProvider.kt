package xyz.xenondevs.nova.world.block.tileentity.network

import org.bukkit.Chunk
import org.bukkit.block.Block
import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkNode

/**
 * A snapshot of the network nodes and unknown blocks in a chunk.
 *
 * @property nodes The discovered network nodes, keyed by their block.
 * @property unknownBlocks Tile entity blocks from addons that were not loaded.
 * This information may be used by the network system to determine whether to delete network data or not.
 */
data class NetworkNodeSnapshot(
    val nodes: Map<Block, NetworkNode>,
    val unknownBlocks: Set<Block>
) {
    
    /**
     * Combines this snapshot with [other].
     */
    operator fun plus(other: NetworkNodeSnapshot): NetworkNodeSnapshot =
        NetworkNodeSnapshot(nodes + other.nodes, unknownBlocks + other.unknownBlocks)
    
    companion object {
        
        /**
         * An empty [NetworkNodeSnapshot].
         */
        val EMPTY = NetworkNodeSnapshot(emptyMap(), emptySet())
        
    }
    
}

/**
 * Used to discover [NetworkNodes][NetworkNode].
 */
interface NetworkNodeProvider {
    
    /**
     * Gets the live [NetworkNode] represented by [block], or null if there is none.
     * Must be called from the server thread.
     */
    fun getNode(block: Block): NetworkNode?
    
    /**
     * Creates and returns a snapshot of all [NetworkNodes][NetworkNode] and unknown blocks in [chunk].
     * Must be called from the server thread.
     */
    fun getNodes(chunk: Chunk): NetworkNodeSnapshot
    
}

