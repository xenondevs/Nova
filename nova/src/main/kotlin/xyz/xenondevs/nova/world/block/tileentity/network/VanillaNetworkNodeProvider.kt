package xyz.xenondevs.nova.world.block.tileentity.network

import net.minecraft.world.level.chunk.LevelChunk
import net.minecraft.world.level.chunk.status.ChunkStatus
import org.bukkit.Chunk
import org.bukkit.block.Block
import org.bukkit.craftbukkit.CraftChunk
import xyz.xenondevs.nova.util.nmsBlockEntity
import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkEndPoint
import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkNode
import xyz.xenondevs.nova.world.block.tileentity.vanilla.VanillaTileEntity

internal object VanillaNetworkNodeProvider : NetworkNodeProvider {
    
    override fun getNode(block: Block): NetworkNode? =
        block.nmsBlockEntity?.let(VanillaTileEntity::of) as? NetworkNode
    
    override fun getNodes(chunk: Chunk): NetworkNodeSnapshot {
        val levelChunk = (chunk as CraftChunk).getHandle(ChunkStatus.FULL) as LevelChunk
        val nodes = levelChunk.blockEntities.values
            .asSequence()
            .mapNotNull(VanillaTileEntity::of)
            .filterIsInstance<NetworkEndPoint>()
            .associateBy(NetworkEndPoint::block)
        
        return NetworkNodeSnapshot(nodes, emptySet())
    }
    
}