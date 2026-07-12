package xyz.xenondevs.nova.world.block.tileentity.network

import org.bukkit.block.Block
import xyz.xenondevs.nova.world.ChunkPos
import xyz.xenondevs.nova.world.block.DefaultBlocks
import xyz.xenondevs.nova.world.block.blockType
import xyz.xenondevs.nova.world.block.novaTileEntity
import xyz.xenondevs.nova.world.block.tileentity.TileEntity
import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkNode

/**
 * Used to discover [NetworkNodes][NetworkNode].
 */
interface NetworkNodeProvider {
    
    /**
     * Gets the [NetworkNode] at the specified block [block] or null if there is none.
     */
    suspend fun getNode(block: Block): NetworkNode?
    
    /**
     * Gets all [NetworkNodes][NetworkNode] in the specified chunk [pos].
     */
    suspend fun getNodes(pos: ChunkPos): Sequence<NetworkNode>
    
    /**
     * Checks whether the block at the specified [block] is unknown.
     * For example, blocks of addons that weren't loaded but may be a [NetworkNode] should be considered unknown.
     *
     * This information may be used by the network system to determine whether to delete network data or not.
     */
    suspend fun isUnknown(block: Block): Boolean = false
    
}

/**
 * A [NetworkNodeProvider] for all [TileEntities][TileEntity] that are [NetworkNodes][NetworkNode].
 */
internal object NovaNetworkNodeProvider : NetworkNodeProvider {
    
    override suspend fun getNode(block: Block): NetworkNode? {
        return block.novaTileEntity as? NetworkNode
    }
    
    override suspend fun isUnknown(block: Block): Boolean {
        return block.blockType == DefaultBlocks.UNKNOWN
    }
    
    override suspend fun getNodes(pos: ChunkPos): Sequence<NetworkNode> {
        // TODO
        return sequenceOf()
    }
    
}