package xyz.xenondevs.nova.world.block.tileentity.network.task

import jdk.jfr.Category
import jdk.jfr.Event
import jdk.jfr.Label
import jdk.jfr.Name
import xyz.xenondevs.commons.guava.component1
import xyz.xenondevs.commons.guava.component2
import xyz.xenondevs.commons.guava.component3
import xyz.xenondevs.commons.guava.iterator
import xyz.xenondevs.nova.LOGGER
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
        val networkChunk = state.storage.getOrLoadNetworkRegion(chunkPos).getChunk(chunkPos)
        val networkNodes = networkChunk.getData()
        
        for ((pos, data) in networkNodes) {
            val node = chunkNodes[pos]
            
            // the network data of unknown nodes should not be removed in order to prevent data loss of addons that weren't loaded
            if (node == null && NetworkManager.isUnknown(pos))
                continue
            
            when {
                node != null && node in state -> {
                    LOGGER.error("Node at pos $pos is already loaded", Exception())
                    continue
                }
                
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
                
                else -> {
                    // node is null or node and data type do not match
                    LOGGER.error("Node type and data type mismatch: $node does not match $data. (Removing from network data storage)", Exception())
                    networkChunk.setData(pos, null)
                    continue
                }
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
    
    override fun toString(): String {
        return "LoadChunkTask(pos=$chunkPos)"
    }
    
}