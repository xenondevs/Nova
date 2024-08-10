package xyz.xenondevs.nova.world.block.tileentity.network.task

import jdk.jfr.Category
import jdk.jfr.Event
import jdk.jfr.Label
import jdk.jfr.Name
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import xyz.xenondevs.commons.guava.component1
import xyz.xenondevs.commons.guava.component2
import xyz.xenondevs.commons.guava.component3
import xyz.xenondevs.commons.guava.iterator
import xyz.xenondevs.nova.world.block.tileentity.network.NetworkManager
import xyz.xenondevs.nova.world.block.tileentity.network.ProtoNetwork
import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkBridge
import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkEndPoint
import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkNode
import xyz.xenondevs.nova.world.ChunkPos
import xyz.xenondevs.nova.world.format.NetworkState
import xyz.xenondevs.nova.world.format.chunk.NetworkBridgeData
import xyz.xenondevs.nova.world.format.chunk.NetworkEndPointData
import java.util.concurrent.ConcurrentHashMap

internal class LoadChunkTask(
    state: NetworkState,
    override val chunkPos: ChunkPos,
) : NetworkTask(state) {
    
    //<editor-fold desc="jfr event", defaultstate="collapsed">
    @Suppress("unused")
    @Name("xyz.xenondevs.LoadChunk")
    @Label("Load Chunk")
    @Category("Nova", "TileEntity Network")
    private inner class LoadChunkTaskEvent : Event() {
        
        @Label("Position")
        val pos: String = this@LoadChunkTask.chunkPos.toString()
        
    }
    
    override val event: Event = LoadChunkTaskEvent()
    //</editor-fold>
    
    override suspend fun run(): Boolean {
        val updatedNetworks = ConcurrentHashMap<ProtoNetwork<*>, MutableSet<NetworkNode>>()
        val networkLessNodes = ConcurrentHashMap.newKeySet<NetworkNode>()
        
        coroutineScope {
            val chunkNodes = NetworkManager.getNodes(chunkPos).associateByTo(HashMap(), NetworkNode::pos)
            val networkNodes = state.storage.getOrLoadNetworkRegion(chunkPos).getChunk(chunkPos).getData()
            for ((pos, data) in networkNodes) {
                val node = chunkNodes[pos]
                if (node == null || node in state)
                    continue
                
                launch(Dispatchers.IO) { // state.resolveOrLoadNetwork likely causes file read
                    when {
                        node is NetworkBridge && data is NetworkBridgeData -> {
                            val networks = data.networks
                            if (networks.isNotEmpty()) {
                                for ((_, id) in networks) {
                                    val network = state.resolveNetwork(id)
                                    updatedNetworks.compute(network) { _, nodes -> nodes?.also { it += node } ?: hashSetOf(node) }
                                }
                            } else {
                                networkLessNodes += node
                            }
                        }
                        
                        node is NetworkEndPoint && data is NetworkEndPointData -> {
                            val networks = data.networks
                            if (!networks.isEmpty) {
                                for ((_, _, id) in networks) {
                                    val network = state.resolveNetwork(id)
                                    updatedNetworks.compute(network) { _, nodes -> nodes?.also { it += node } ?: hashSetOf(node) }
                                }
                            } else {
                                networkLessNodes += node
                            }
                        }
                        
                        else -> throw IllegalStateException("Node type and data type do not match")
                    }
                }
            }
        }
        
        for (node in networkLessNodes) {
            state += node
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