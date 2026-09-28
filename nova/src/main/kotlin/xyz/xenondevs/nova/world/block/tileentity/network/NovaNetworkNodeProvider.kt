package xyz.xenondevs.nova.world.block.tileentity.network

import org.bukkit.Chunk
import org.bukkit.block.Block
import xyz.xenondevs.nova.world.block.novaTileEntities
import xyz.xenondevs.nova.world.block.novaTileEntity
import xyz.xenondevs.nova.world.block.tileentity.UnknownTileEntity
import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkNode

internal object NovaNetworkNodeProvider : NetworkNodeProvider {
    
    override fun getNode(block: Block): NetworkNode? =
        block.novaTileEntity as? NetworkNode
    
    override fun getNodes(chunk: Chunk): NetworkNodeSnapshot {
        val nodes = HashMap<Block, NetworkNode>()
        val unknownBlocks = HashSet<Block>()
        
        for (tileEntity in chunk.novaTileEntities) {
            if (tileEntity is NetworkNode)
                nodes[tileEntity.block] = tileEntity
            if (tileEntity is UnknownTileEntity)
                unknownBlocks += tileEntity.block
        }
        
        return NetworkNodeSnapshot(nodes, unknownBlocks)
    }
    
}