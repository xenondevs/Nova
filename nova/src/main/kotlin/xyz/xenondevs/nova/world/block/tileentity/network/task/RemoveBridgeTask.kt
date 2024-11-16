package xyz.xenondevs.nova.world.block.tileentity.network.task

import jdk.jfr.Category
import jdk.jfr.Event
import jdk.jfr.Label
import jdk.jfr.Name
import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.tileentity.network.ProtoNetwork
import xyz.xenondevs.nova.world.block.tileentity.network.node.GhostNetworkNode
import xyz.xenondevs.nova.world.block.tileentity.network.node.MutableNetworkNodeConnection
import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkBridge
import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkEndPoint
import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkNode
import xyz.xenondevs.nova.world.block.tileentity.network.type.NetworkType
import xyz.xenondevs.nova.world.format.NetworkState
import xyz.xenondevs.nova.world.format.chunk.NetworkBridgeData
import xyz.xenondevs.nova.world.format.chunk.NetworkEndPointData
import java.util.*

internal class RemoveBridgeTask(
    state: NetworkState,
    node: NetworkBridge,
    updateNodes: Boolean
) : RemoveNodeTask<NetworkBridge>(state, node, updateNodes) {
    
    //<editor-fold desc="jfr event", defaultstate="collapsed">
    @Suppress("unused")
    @Name("xyz.xenondevs.RemoveBridge")
    @Label("Remove Bridge")
    @Category("Nova", "TileEntity Network")
    private inner class AddBridgeTaskEvent : Event() {
        
        @Label("Position")
        val pos: String = node.pos.toString()
        
    }
    
    override val event: Event = AddBridgeTaskEvent()
    //</editor-fold>
    
    override suspend fun remove() {
        for ((networkType, currentNetworkId) in state.getNetworks(node)) {
            val currentNetwork = state.getNetworkOrThrow(networkType, currentNetworkId)
            
            val connectedBridges = HashSet<NetworkBridge>()
            val connectedEndPoints = HashSet<NetworkEndPoint>()
            disconnectConnectedNodes(currentNetwork, connectedBridges, connectedEndPoints)
            
            if (connectedBridges.size > 1) { // destroying this bridge might split network, recalculation required
                val recalculatedNetworkLayouts = recalculateNetworks(node, connectedBridges, networkType)
                if (recalculatedNetworkLayouts != null) { // null means no split in networks
                    val recalculatedNetworks = recalculatedNetworkLayouts.map { nodes ->
                        ProtoNetwork(state, networkType, nodes = nodes.filterTo(HashMap()) { (_, con) -> con.node !is GhostNetworkNode }) 
                    }
                    state -= currentNetwork
                    state += recalculatedNetworks
                    reassignNetworks(recalculatedNetworkLayouts, recalculatedNetworks)
                    clustersToInit += recalculatedNetworks
                    reclusterize(currentNetwork)
                } else {
                    currentNetwork.removeNode(node) // network empty check not required because >1 connected bridges
                    if (connectedEndPoints.isNotEmpty()) { // networks have not been split, only detached end points could de-cluster
                        reclusterize(currentNetwork)
                    }
                }
            } else {
                currentNetwork.removeNode(node)
                
                if (currentNetwork.isEmpty()) {
                    state -= currentNetwork
                    reclusterize(currentNetwork)
                } else if (connectedEndPoints.isNotEmpty()) { // networks have not been split, only detached end points could de-cluster
                    reclusterize(currentNetwork)
                }
            }
        }
    }
    
    /**
     * Disconnects all directly nodes connected over [network] from this bridge.
     * Writes all previously connected nodes [nodesToUpdate], and also all
     * [NetworkBridges][NetworkBridge] to [connectedBridges] and all
     * [NetworkEndPoints][NetworkEndPoint] to [connectedEndPoints].
     */
    private suspend fun disconnectConnectedNodes(
        network: ProtoNetwork<*>,
        connectedBridges: MutableSet<NetworkBridge>,
        connectedEndPoints: MutableSet<NetworkEndPoint>
    ) {
        state.forEachConnectedNode(node, network.type) { face, connectedNode ->
            val oppositeFace = face.oppositeFace
            when (connectedNode) {
                is NetworkEndPoint -> {
                    disconnectEndPoint(connectedNode, oppositeFace, network)
                    connectedEndPoints += connectedNode
                }
                
                is NetworkBridge -> {
                    disconnectBridge(connectedNode, oppositeFace, network)
                    connectedBridges += connectedNode
                }
            }
            nodesToUpdate += connectedNode
        }
    }
    
    /**
     * Disconnects [endPoint] at [face] from [network].
     */
    private suspend fun disconnectEndPoint(endPoint: NetworkEndPoint, face: BlockFace, network: ProtoNetwork<*>) {
        val networkType = network.type
        state.removeNetwork(endPoint, networkType, face)
        state.removeConnection(endPoint, networkType, face)
        network.removeFace(endPoint, face)
    }
    
    /**
     * Disconnects [bridge] at [face] from [network].
     */
    private suspend fun disconnectBridge(bridge: NetworkBridge, face: BlockFace, network: ProtoNetwork<*>) {
        state.removeConnection(bridge, network.type, face)
    }
    
    /**
     * Reassigns the networks inside [NetworkBridgeData.networks] and [NetworkEndPointData.networks]
     * for all nodes in [layouts], assuming [layouts] indices correspond the [networks] indices.
     */
    private suspend fun reassignNetworks(
        layouts: List<Map<BlockPos, MutableNetworkNodeConnection>>,
        networks: List<ProtoNetwork<*>>
    ) {
        for ((i, layout) in layouts.withIndex()) {
            val network = networks[i]
            for ((node, faces) in layout.values) {
                when (node) {
                    is NetworkBridge -> state.setNetwork(node, network)
                    is NetworkEndPoint -> state.setNetwork(node, faces, network)
                }
            }
        }
    }
    
    /**
     * Recalculates networks for the case that [bridge] was destroyed and previously connected to at least two other bridges, stored in [connectedPreviously].
     * It is assumed, that [bridge] has been removed from the connections of attached [NetworkNodes][NetworkNode].
     *
     * @return A list of new network layouts, or null if the networks haven't been split.
     * Will not contain duplicates. May contain [GhostNetworkNodes][GhostNetworkNode], which need to be removed before creating a [ProtoNetwork].
     */
    private suspend fun recalculateNetworks(
        bridge: NetworkBridge,
        connectedPreviously: Set<NetworkBridge>,
        networkType: NetworkType<*>
    ): List<Map<BlockPos, MutableNetworkNodeConnection>>? {
        require(connectedPreviously.size > 1) { "Recalculating networks is not required" }
        
        val potentialNetworks = ArrayList<MutableMap<BlockPos, MutableNetworkNodeConnection>>()
        val previouslyExploredBridges = HashSet<NetworkBridge>() // used for detecting identical side iterations
        
        state.forEachConnectedNode(bridge, networkType) sideIteration@{ startFace, startNode ->
            val potentialNetwork = HashMap<BlockPos, MutableNetworkNodeConnection>()
            val exploredNodes = HashSet<NetworkNode>()
            val exploredBridges = HashSet<NetworkBridge>()
            val remainingConnectedPreviously = HashSet(connectedPreviously)
            
            val queue = LinkedList<Pair<BlockFace, NetworkNode>>() // <ApproachingFace, Node>
            queue.add(startFace to startNode)
            exploredNodes += startNode
            
            while (queue.isNotEmpty()) {
                val (approachingFace, currentNode) = queue.poll()
                
                if (currentNode is NetworkBridge) {
                    // if we can reach all previously connected bridges in one side iteration, the networks will not be split
                    remainingConnectedPreviously -= currentNode
                    if (remainingConnectedPreviously.isEmpty())
                        return null
                    
                    state.forEachConnectedNode(currentNode, networkType) { face, neighbor ->
                        // if this bridge was already explored when we started from a different side,
                        // this run will just be a duplicate of a previous side iteration and should be stopped here
                        if (neighbor in previouslyExploredBridges)
                            return@sideIteration
                        
                        if (neighbor !in exploredNodes) {
                            queue += face to neighbor
                            exploredNodes += neighbor
                        } else if (neighbor is NetworkEndPoint) {
                            // the connection from a different side might still be important
                            potentialNetwork.getOrPut(neighbor.pos) {
                                MutableNetworkNodeConnection(neighbor)
                            }.faces += face.oppositeFace
                        }
                    }
                    
                    exploredBridges += currentNode
                }
                
                // node is now explored, add to network content
                potentialNetwork.getOrPut(currentNode.pos) {
                    MutableNetworkNodeConnection(currentNode)
                }.faces += approachingFace.oppositeFace
            }
            
            if (potentialNetwork.size > 1 || potentialNetwork.values.any { it.node is NetworkBridge }) {
                potentialNetworks += potentialNetwork
            }
            previouslyExploredBridges += exploredBridges
        }
        
        return potentialNetworks
    }
    
}