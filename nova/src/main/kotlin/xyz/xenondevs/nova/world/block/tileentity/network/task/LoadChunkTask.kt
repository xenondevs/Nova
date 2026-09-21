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
import xyz.xenondevs.nova.world.block.tileentity.network.NetworkNodeSnapshot
import xyz.xenondevs.nova.world.block.tileentity.network.ProtoNetwork
import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkBridge
import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkEndPoint
import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkNode
import xyz.xenondevs.nova.world.block.tileentity.network.node.safelyHandleNetworkLoaded
import xyz.xenondevs.nova.world.format.NetworkState
import xyz.xenondevs.nova.world.format.chunk.NetworkBridgeData
import xyz.xenondevs.nova.world.format.chunk.NetworkEndPointData

internal class LoadChunkTask(
    state: NetworkState,
    override val chunkPos: ChunkPos,
    private val snapshot: NetworkNodeSnapshot
) : NetworkTask(state) {
    
    //<editor-fold desc="jfr event", defaultstate="collapsed">
    @Suppress("unused")
    @Name("xyz.xenondevs.LoadChunk")
    @Label("Load Chunk")
    @Category("Nova", "TileEntity Network")
    private inner class LoadChunkTaskEvent : Event() {
        
        @Label("Position")
        val pos: String = chunkPos.toString()
        
    }
    
    override val event: Event = LoadChunkTaskEvent()
    //</editor-fold>
    
    override suspend fun run(): Boolean {
        val updatedNetworks = HashMap<ProtoNetwork<*>, MutableSet<NetworkNode>>()
        
        val networkChunk = state.storage.getOrLoadRegionizedChunk(chunkPos)
        val networkNodes = networkChunk.getData()
        
        for ([pos, data] in networkNodes) {
            val node = snapshot.nodes[pos]
            
            // the network data of unknown nodes should not be removed in order to prevent data loss of addons that weren't loaded
            if (node == null && pos in snapshot.unknownBlocks)
                continue
            
            when {
                node != null && node in state -> {
                    LOGGER.error("Error while loading network chunk at $chunkPos: Node at pos $pos is already loaded")
                    continue
                }
                
                node is NetworkBridge && data is NetworkBridgeData -> {
                    val networks = data.networks
                    for ([type, id] in networks) {
                        val network = state.getOrCreateNetwork(type, id)
                        network.addBridge(node)
                        updatedNetworks.getOrPut(network, ::HashSet) += node
                    }
                }
                
                node is NetworkEndPoint && data is NetworkEndPointData -> {
                    val networks = data.networks
                    for ([type, face, id] in networks) {
                        val network = state.getOrCreateNetwork(type, id)
                        network.addEndPoint(node, face)
                        updatedNetworks.getOrPut(network, ::HashSet) += node
                    }
                }
                
                else -> {
                    // node is null or node and data type do not match
                    if (node == null) {
                        LOGGER.error("Error while loading network chunk at $chunkPos: Expected node at $pos, but found none. (Removing from network data storage)")
                    } else {
                        LOGGER.error("Error while loading network chunk at $chunkPos: Node type and data type mismatch: $node does not match $data. (Removing from network data storage)")
                    }
                    networkChunk.setData(pos, null)
                    continue
                }
            }
            
            state += node
        }
        
        for ([network, nodes] in updatedNetworks) {
            for (node in nodes) {
                node.safelyHandleNetworkLoaded(state)
            }
            
            network.cluster?.invalidate()
        }
        
        return updatedNetworks.isNotEmpty()
    }
    
    override fun toString(): String {
        return "LoadChunkTask(pos=$chunkPos)"
    }
    
}
