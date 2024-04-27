package xyz.xenondevs.nova.tileentity.network

import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.tileentity.network.node.NetworkNode
import xyz.xenondevs.nova.tileentity.vanilla.VanillaTileEntity
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.ChunkPos
import xyz.xenondevs.nova.world.format.WorldDataManager

/**
 * Used to discover [NetworkNodes][NetworkNode].
 */
interface NetworkNodeProvider {
    
    /**
     * Gets the [NetworkNode] at the specified block [pos] or null if there is none.
     */
    fun getNode(pos: BlockPos): NetworkNode?
    
    /**
     * Gets all [NetworkNodes][NetworkNode] in the specified chunk [pos].
     */
    fun getNodes(pos: ChunkPos): Sequence<NetworkNode>
    
}

/**
 * A [NetworkNodeProvider] for all [TileEntities][TileEntity] that are [NetworkNodes][NetworkNode].
 */
internal object NovaNetworkNodeProvider : NetworkNodeProvider {
    
    override fun getNode(pos: BlockPos): NetworkNode? {
        return WorldDataManager.getTileEntity(pos) as? NetworkNode
    }
    
    override fun getNodes(pos: ChunkPos): Sequence<NetworkNode> {
        return WorldDataManager.getTileEntities(pos)
            .asSequence()
            .filterIsInstance<NetworkNode>()
    }
    
}

/**
 * A [NetworkNodeProvider] for all vanilla [VanillaTileEntities][VanillaTileEntity] that are [NetworkNodes][NetworkNode].
 
 */
internal object VanillaNetworkNodeProvider : NetworkNodeProvider {
    
    override fun getNode(pos: BlockPos): NetworkNode? {
        return WorldDataManager.getVanillaTileEntity(pos) as? NetworkNode
    }
    
    override fun getNodes(pos: ChunkPos): Sequence<NetworkNode> {
        return WorldDataManager.getVanillaTileEntities(pos)
            .asSequence()
            .filterIsInstance<NetworkNode>()
    }
    
}