package xyz.xenondevs.nova.world.block.tileentity.network

import xyz.xenondevs.nova.world.block.tileentity.TileEntity
import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkNode
import xyz.xenondevs.nova.world.block.tileentity.vanilla.VanillaTileEntity
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
    suspend fun getNode(pos: BlockPos): NetworkNode?
    
    /**
     * Gets all [NetworkNodes][NetworkNode] in the specified chunk [pos].
     */
    suspend fun getNodes(pos: ChunkPos): Sequence<NetworkNode>
    
}

/**
 * A [NetworkNodeProvider] for all [TileEntities][TileEntity] that are [NetworkNodes][NetworkNode].
 */
internal object NovaNetworkNodeProvider : NetworkNodeProvider {
    
    override suspend fun getNode(pos: BlockPos): NetworkNode? {
        return WorldDataManager.getOrLoadTileEntity(pos) as? NetworkNode
    }
    
    override suspend fun getNodes(pos: ChunkPos): Sequence<NetworkNode> {
        return WorldDataManager.getOrLoadTileEntities(pos)
            .asSequence()
            .filterIsInstance<NetworkNode>()
    }
    
}

/**
 * A [NetworkNodeProvider] for all vanilla [VanillaTileEntities][VanillaTileEntity] that are [NetworkNodes][NetworkNode].
 
 */
internal object VanillaNetworkNodeProvider : NetworkNodeProvider {
    
    override suspend fun getNode(pos: BlockPos): NetworkNode? {
        return WorldDataManager.getOrLoadVanillaTileEntity(pos) as? NetworkNode
    }
    
    override suspend fun getNodes(pos: ChunkPos): Sequence<NetworkNode> {
        return WorldDataManager.getOrLoadVanillaTileEntities(pos)
            .asSequence()
            .filterIsInstance<NetworkNode>()
    }
    
}