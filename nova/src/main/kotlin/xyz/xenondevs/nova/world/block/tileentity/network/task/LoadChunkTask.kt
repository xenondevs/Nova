package xyz.xenondevs.nova.world.block.tileentity.network.task

import jdk.jfr.Category
import jdk.jfr.Event
import jdk.jfr.Label
import jdk.jfr.Name
import xyz.xenondevs.commons.guava.component1
import xyz.xenondevs.commons.guava.component2
import xyz.xenondevs.commons.guava.component3
import xyz.xenondevs.commons.guava.iterator
import xyz.xenondevs.nova.world.ChunkPos
import xyz.xenondevs.nova.world.block.tileentity.network.NetworkManager
import xyz.xenondevs.nova.world.block.tileentity.network.ProtoNetwork
import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkBridge
import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkEndPoint
import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkNode
import xyz.xenondevs.nova.world.format.NetworkState
import xyz.xenondevs.nova.world.format.chunk.NetworkBridgeData
import xyz.xenondevs.nova.world.format.chunk.NetworkEndPointData

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
        val updatedNetworks = HashMap<ProtoNetwork<*>, MutableSet<NetworkNode>>()
        
        val chunkNodes = NetworkManager.getNodes(chunkPos).associateByTo(HashMap(), NetworkNode::pos)
        val networkNodes = state.storage.getOrLoadNetworkRegion(chunkPos).getChunk(chunkPos).getData()
        
        for ((pos, data) in networkNodes) {
            val node = chunkNodes[pos]
            if (node == null || node in state)
                continue
            
            when {
                node is NetworkBridge && data is NetworkBridgeData -> {
                    val networks = data.networks
                    for ((type, id) in networks) {
                        val network = state.getOrCreateNetwork(type, id)
                        network.addBridge(node)
                        updatedNetworks.getOrPut(network, ::HashSet) += node
                    }
                }
                
                node is NetworkEndPoint && data is NetworkEndPointData -> {
                    val networks = data.networks
                    for ((type, face, id) in networks) {
                        val network = state.getOrCreateNetwork(type, id)
                        network.addEndPoint(node, face)
                        updatedNetworks.getOrPut(network, ::HashSet) += node
                    }
                }
                
                else -> throw IllegalStateException("Node type and data type do not match")
            }
            
            state += node
        }
        
        for ((network, nodes) in updatedNetworks) {
            for (node in nodes) {
                node.handleNetworkLoaded(state)
            }
            
            network.enlargeCluster(nodes)
        }
        
        return updatedNetworks.isNotEmpty()
    }
    
}