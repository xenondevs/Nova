package xyz.xenondevs.nova.tileentity.network.task

import xyz.xenondevs.commons.guava.component1
import xyz.xenondevs.commons.guava.component2
import xyz.xenondevs.commons.guava.component3
import xyz.xenondevs.commons.guava.iterator
import xyz.xenondevs.nova.tileentity.network.node.NetworkEndPoint
import xyz.xenondevs.nova.world.format.NetworkState

internal class RemoveEndPointTask(
    state: NetworkState,
    node: NetworkEndPoint,
    updateNodes: Boolean
) : RemoveNodeTask<NetworkEndPoint>(state, node, updateNodes) {
    
    override fun remove() {
        // remove this endpoint from the connectedNodes map of all connected nodes
        state.forEachConnectedNode(node) { type, face, connectedNode ->
            state.removeConnection(connectedNode, type, face.oppositeFace)
            nodesToUpdate += connectedNode
        }
        
        // remove endpoint from all networks
        for ((_, _, networkId) in state.getNetworks(node)) {
            val network = state.resolveNetwork(networkId)
            network.removeNode(node)
            
            if (network.isEmpty()) {
                state -= network
                reclusterize(network)
            } else {
                reclusterize(network)
            }
        }
    }
    
}