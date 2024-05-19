package xyz.xenondevs.nova.tileentity.network.task

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import xyz.xenondevs.commons.guava.component1
import xyz.xenondevs.commons.guava.component2
import xyz.xenondevs.commons.guava.component3
import xyz.xenondevs.commons.guava.iterator
import xyz.xenondevs.nova.tileentity.network.NetworkManager
import xyz.xenondevs.nova.tileentity.network.ProtoNetwork
import xyz.xenondevs.nova.tileentity.network.node.NetworkBridge
import xyz.xenondevs.nova.tileentity.network.node.NetworkEndPoint
import xyz.xenondevs.nova.tileentity.network.node.NetworkNode
import xyz.xenondevs.nova.world.ChunkPos
import xyz.xenondevs.nova.world.format.NetworkState
import xyz.xenondevs.nova.world.format.chunk.NetworkBridgeData
import xyz.xenondevs.nova.world.format.chunk.NetworkEndPointData
import java.util.concurrent.ConcurrentHashMap

internal class LoadChunkTask(
    state: NetworkState,
    private val pos: ChunkPos,
) : NetworkTask(state) {
    
    override suspend fun run(): Boolean {
        val updatedNetworks = ConcurrentHashMap<ProtoNetwork<*>, MutableSet<NetworkNode>>()
        
        coroutineScope {
            val chunkNodes = NetworkManager.getNodes(pos).associateByTo(HashMap(), NetworkNode::pos)
            val networkNodes = state.storage.getOrLoadNetworkRegion(pos).getChunk(pos).getData()
            for ((pos, data) in networkNodes) {
                val node = chunkNodes[pos]
                if (node == null || node in state)
                    continue
                
                launch(Dispatchers.IO) { // state.resolveOrLoadNetwork likely causes file read
                    when {
                        node is NetworkBridge && data is NetworkBridgeData -> {
                            for ((_, id) in data.networks) {
                                val network = state.resolveOrLoadNetwork(id)
                                updatedNetworks.compute(network) { _, nodes -> nodes?.also { it += node } ?: hashSetOf(node) }
                            }
                        }
                        
                        node is NetworkEndPoint && data is NetworkEndPointData -> {
                            for ((_, _, id) in data.networks) {
                                val network = state.resolveOrLoadNetwork(id)
                                updatedNetworks.compute(network) { _, nodes -> nodes?.also { it += node } ?: hashSetOf(node) }
                            }
                        }
                        
                        else -> throw IllegalStateException("Node type and data type do not match")
                    }
                }
            }
        }
        
        for ((network, nodes) in updatedNetworks) {
            for (node in nodes) {
                state += node
                network.loadNode(node)
                node.handleNetworkLoaded(state)
            }
            
            network.enlargeCluster(nodes)
        }
        
        return updatedNetworks.isNotEmpty()
    }
    
}