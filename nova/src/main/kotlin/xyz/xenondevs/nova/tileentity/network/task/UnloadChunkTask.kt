package xyz.xenondevs.nova.tileentity.network.task

import xyz.xenondevs.nova.tileentity.network.NetworkManager
import xyz.xenondevs.nova.tileentity.network.ProtoNetwork
import xyz.xenondevs.nova.tileentity.network.node.NetworkBridge
import xyz.xenondevs.nova.tileentity.network.node.NetworkEndPoint
import xyz.xenondevs.nova.tileentity.network.node.NetworkNode
import xyz.xenondevs.nova.world.ChunkPos
import xyz.xenondevs.nova.world.format.NetworkState
import xyz.xenondevs.nova.world.format.chunk.NetworkBridgeData
import xyz.xenondevs.nova.world.format.chunk.NetworkEndPointData

internal class UnloadChunkTask(
    state: NetworkState,
    private val pos: ChunkPos,
) : NetworkTask(state) {
    
    override suspend fun run(): Boolean {
        val clustersToInit = HashSet<ProtoNetwork<*>>()
        
        fun remove(node: NetworkNode, network: ProtoNetwork<*>) {
            network.unloadNode(node)
            network.cluster?.forEach { previouslyClusteredNetwork ->
                previouslyClusteredNetwork.invalidateCluster()
                clustersToInit += previouslyClusteredNetwork
            }
        }
        
        val chunkNodes = NetworkManager.getNodes(pos).associateByTo(HashMap(), NetworkNode::pos)
        val networkNodes = state.storage.getNetworkChunkOrThrow(pos).getData()
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
                    state.forEachNetwork(node) { _, _ , network -> remove(node, network) }
                
                else -> throw IllegalStateException("Node type and data type do not match")
            }
        }
        
        for (network in clustersToInit) {
            if (network.isUnloaded()) {
                // TODO: save on unload
//                state -= network
            } else {
                network.initCluster()
            }
        }
        
        return true
    }
    
}