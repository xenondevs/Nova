package xyz.xenondevs.nova.world.block.tileentity.network.task

import jdk.jfr.Category
import jdk.jfr.Event
import jdk.jfr.Label
import jdk.jfr.Name
import xyz.xenondevs.commons.guava.component1
import xyz.xenondevs.commons.guava.component2
import xyz.xenondevs.commons.guava.component3
import xyz.xenondevs.commons.guava.iterator
import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkEndPoint
import xyz.xenondevs.nova.world.format.NetworkState

internal class RemoveEndPointTask(
    state: NetworkState,
    node: NetworkEndPoint,
    updateNodes: Boolean
) : RemoveNodeTask<NetworkEndPoint>(state, node, updateNodes) {
    
    //<editor-fold desc="jfr event", defaultstate="collapsed">
    @Suppress("unused")
    @Name("xyz.xenondevs.RemoveEndPoint")
    @Label("Remove EndPoint")
    @Category("Nova", "TileEntity Network")
    private inner class RemoveEndPointTaskEvent : Event() {
        
        @Label("Position")
        val pos: String = node.pos.toString()
        
    }
    
    override val event: Event = RemoveEndPointTaskEvent()
    //</editor-fold>
    
    override suspend fun remove() {
        // remove this endpoint from the connectedNodes map of all connected nodes
        state.forEachConnectedNode(node) { type, face, connectedNode ->
            state.removeConnection(connectedNode, type, face.oppositeFace)
            if (connectedNode is NetworkEndPoint) {
                state.removeNetwork(connectedNode, type, face.oppositeFace)
            }
            nodesToUpdate += connectedNode
        }
        
        // remove endpoint from all networks
        for ((_, _, networkId) in state.getNetworks(node)) {
            val network = state.resolveNetwork(networkId)
            network.removeNode(node)
            
            if (network.isEmpty()) {
                state.deleteNetwork(network)
                reclusterize(network)
            } else {
                reclusterize(network)
            }
        }
    }
    
}