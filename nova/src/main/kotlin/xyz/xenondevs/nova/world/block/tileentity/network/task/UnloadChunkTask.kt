package xyz.xenondevs.nova.world.block.tileentity.network.task

import jdk.jfr.Category
import jdk.jfr.Event
import jdk.jfr.Label
import jdk.jfr.Name
import xyz.xenondevs.nova.world.ChunkPos
import xyz.xenondevs.nova.world.block.tileentity.network.NetworkManager
import xyz.xenondevs.nova.world.block.tileentity.network.ProtoNetwork
import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkBridge
import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkEndPoint
import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkNode
import xyz.xenondevs.nova.world.format.NetworkState
import xyz.xenondevs.nova.world.format.chunk.NetworkBridgeData
import xyz.xenondevs.nova.world.format.chunk.NetworkEndPointData

internal class UnloadChunkTask(
    state: NetworkState,
    override val chunkPos: ChunkPos,
) : NetworkTask(state) {
    
    //<editor-fold desc="jfr event", defaultstate="collapsed">
    @Suppress("unused")
    @Name("xyz.xenondevs.UnloadChunkTask")
    @Label("Unload Chunk")
    @Category("Nova", "TileEntity Network")
    private inner class UnloadChunkTaskEvent : Event() {
        
        @Label("Position")
        val pos: String = this@UnloadChunkTask.chunkPos.toString()
        
    }
    
    override val event: Event = UnloadChunkTaskEvent()
    //</editor-fold>
    
    override suspend fun run(): Boolean {
        val clustersToInit = HashSet<ProtoNetwork<*>>()
        
        fun remove(node: NetworkNode, network: ProtoNetwork<*>) {
            network.removeNode(node)
            network.cluster?.forEach { previouslyClusteredNetwork ->
                previouslyClusteredNetwork.invalidateCluster()
                clustersToInit += previouslyClusteredNetwork
            }
        }
        
        val chunkNodes = NetworkManager.getNodes(chunkPos).associateByTo(HashMap(), NetworkNode::pos)
        val networkNodes = state.storage.getRegionizedChunkOrThrow(chunkPos).getData() // fixme: edge cases where unload & save happen before this task
        if (networkNodes.isEmpty())
            return false
        
        for ((pos, data) in networkNodes) {
            val node = chunkNodes[pos]
            if (node == null || node !in state)
                continue
            
            state -= node
            
            when {
                node is NetworkBridge && data is NetworkBridgeData ->
                    state.forEachNetwork(node) { _, network -> remove(node, network) }
                
                node is NetworkEndPoint && data is NetworkEndPointData ->
                    state.forEachNetwork(node) { _, _, network -> remove(node, network) }
                
                else -> throw IllegalStateException("Node type and data type do not match")
            }
        }
        
        for (network in clustersToInit) {
            if (network.isEmpty()) {
                state -= network
            } else {
                network.initCluster()
            }
        }
        
        return true
    }
    
    override fun toString(): String {
        return "UnloadChunkTask(pos=$chunkPos)"
    }
    
}