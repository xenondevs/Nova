package xyz.xenondevs.nova.world.block.tileentity.network.task

import jdk.jfr.Category
import jdk.jfr.Event
import jdk.jfr.Label
import jdk.jfr.Name
import xyz.xenondevs.nova.world.ChunkPos
import xyz.xenondevs.nova.world.block.tileentity.network.ProtoNetwork
import xyz.xenondevs.nova.world.format.NetworkState

internal class UnloadChunkTask(
    state: NetworkState,
    override val chunkPos: ChunkPos
) : NetworkTask(state) {
    
    //<editor-fold desc="jfr event", defaultstate="collapsed">
    @Suppress("unused")
    @Name("xyz.xenondevs.UnloadChunkTask")
    @Label("Unload Chunk")
    @Category("Nova", "TileEntity Network")
    private inner class UnloadChunkTaskEvent : Event() {
        
        @Label("Position")
        val pos: String = chunkPos.toString()
        
    }
    
    override val event: Event = UnloadChunkTaskEvent()
    //</editor-fold>
    
    override suspend fun run(): Boolean {
        val nodes = state.removeNodes(chunkPos)
        if (nodes.isEmpty())
            return false
        
        // TODO: chunk to networks index
        val emptyNetworks = ArrayList<ProtoNetwork<*>>()
        for (network in state.networks) {
            if (network.nodes.keys.none(nodes::containsKey))
                continue
            
            network.removeAll(nodes.values)
            network.cluster?.invalidate()
            if (network.isEmpty())
                emptyNetworks += network
        }
        
        for (network in emptyNetworks) {
            state -= network
        }
        
        return true
    }
    
    override fun toString(): String {
        return "UnloadChunkTask(pos=$chunkPos)"
    }
    
}
