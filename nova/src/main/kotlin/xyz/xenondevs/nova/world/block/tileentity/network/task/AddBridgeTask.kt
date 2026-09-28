package xyz.xenondevs.nova.world.block.tileentity.network.task

import jdk.jfr.Category
import jdk.jfr.Event
import jdk.jfr.Label
import jdk.jfr.Name
import xyz.xenondevs.commons.guava.replaceAll
import xyz.xenondevs.nova.util.CubeFaceMap
import xyz.xenondevs.nova.util.CubeFaceSet
import xyz.xenondevs.nova.util.forEachNonNull
import xyz.xenondevs.nova.world.block.tileentity.network.Network
import xyz.xenondevs.nova.world.block.tileentity.network.ProtoNetwork
import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkBridge
import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkEndPoint
import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkNode
import xyz.xenondevs.nova.world.block.tileentity.network.type.NetworkType
import xyz.xenondevs.nova.world.format.NetworkState
import xyz.xenondevs.nova.world.format.chunk.NetworkBridgeData
import java.util.*

internal class AddBridgeTask(
    state: NetworkState,
    node: NetworkBridge,
    private val supportedNetworkTypes: Set<NetworkType<*>>,
    private val bridgeFaces: CubeFaceSet,
    updateNodes: Boolean
) : AddNodeTask<NetworkBridge>(state, node, updateNodes) {
    
    //<editor-fold desc="jfr event", defaultstate="collapsed">
    @Suppress("unused")
    @Name("xyz.xenondevs.AddBridge")
    @Label("Add Bridge")
    @Category("Nova", "TileEntity Network")
    private class AddBridgeTaskEvent : Event() {
        
        @Label("Position")
        var pos: String = ""
        
    }
    
    override val event: Event
        get() = AddBridgeTaskEvent()
    
    override fun populateEvent(event: Event) {
        (event as AddBridgeTaskEvent).pos = node.block.toString()
    }
    //</editor-fold>
    
    override suspend fun add() {
        state.setBridgeData(
            node.block,
            NetworkBridgeData(
                typeId = node.typeId,
                owner = node.owner?.uniqueId ?: UUID(0L, 0L),
                supportedNetworkTypes = HashSet(supportedNetworkTypes),
                bridgeFaces = bridgeFaces
            )
        )
        
        val allowedFaces: CubeFaceSet = bridgeFaces and protectionResult
        val nearbyNodes: CubeFaceMap<NetworkNode?> = state.getNearbyNodes(node.block, allowedFaces)
        val nearbyBridges: CubeFaceMap<NetworkBridge?> = nearbyNodes.map { it as? NetworkBridge }
        val nearbyEndPoints: CubeFaceMap<NetworkEndPoint?> = nearbyNodes.map { it as? NetworkEndPoint }
        
        for (networkType in supportedNetworkTypes) {
            val availableBridges = nearbyBridges.filter { face, bridge ->
                face.oppositeFace in state.getAllowedFaces(bridge, networkType) && node.typeId == bridge.typeId
            }
            val availableEndPoints = nearbyEndPoints.filter { face, endPoint ->
                face.oppositeFace in state.getAllowedFaces(endPoint, networkType)
            }
            
            availableBridges.forEachNonNull(nodesToUpdate::add)
            availableEndPoints.forEachNonNull(nodesToUpdate::add)
            
            val network = connectBridgeToBridges(node, availableBridges, networkType)
            connectBridgeToEndPoints(node, availableEndPoints, network)
            network.cluster?.invalidate()
        }
    }
    
    /**
     * Connects the bridge [self] to the given [neighbors] using [network].
     */
    private suspend fun connectBridgeToEndPoints(
        self: NetworkBridge,
        neighbors: CubeFaceMap<NetworkEndPoint?>,
        network: ProtoNetwork<*>
    ) {
        val networkType = network.type
        neighbors.forEachNonNull { face, neighbor ->
            val oppositeFace = face.oppositeFace
            
            // add endpoint to network
            network.addEndPoint(neighbor, oppositeFace)
            
            // remember connections in self and neighbor
            state.setConnection(self, networkType, face)
            state.setConnection(neighbor, networkType, oppositeFace)
            state.setNetwork(neighbor, oppositeFace, network)
        }
    }
    
    /**
     * Connects the bridge [self] to the given [neighbors]. Then returns the network that the bridge is now connected to.
     * Depending on the amount of neighboring [ProtoNetworks][ProtoNetwork], this function my either merge networks,
     * add the bridge to an existing [ProtoNetwork], or create a new [ProtoNetwork].
     */
    private suspend fun <T : Network<T>> connectBridgeToBridges(
        self: NetworkBridge,
        neighbors: CubeFaceMap<NetworkBridge?>,
        networkType: NetworkType<T>
    ): ProtoNetwork<T> {
        val previousNetworks = HashSet<ProtoNetwork<T>>()
        neighbors.forEachNonNull { face, neighbor ->
            previousNetworks += state.getNetwork(neighbor, networkType)!!
            
            // remember connection in self and neighbor
            state.setConnection(self, networkType, face)
            state.setConnection(neighbor, networkType, face.oppositeFace)
        }
        
        // depending on how many networks there are, perform the required action
        val network = when {
            // Merge network
            previousNetworks.size > 1 -> mergeNetworks(networkType, previousNetworks)
            // Connect to existing network
            previousNetworks.size == 1 -> previousNetworks.first()
            // Make a new network
            else -> state.createNetwork(networkType)
        }
        
        // add self to network
        state.setNetwork(self, network)
        network.addBridge(self)
        
        return network
    }
    
    /**
     * Merges the given [networks] into a single network of [type].
     */
    private suspend fun <T : Network<T>> mergeNetworks(
        type: NetworkType<T>,
        networks: Set<ProtoNetwork<T>>
    ): ProtoNetwork<T> {
        // remove old networks
        for (previousNetwork in networks) {
            state -= previousNetwork
        }
        
        // create and populate new network
        val mergedNetwork = state.createNetwork(type)
        for (previousNetwork in networks) {
            mergedNetwork.addAll(previousNetwork)
        }
        reassignNetworks(mergedNetwork, networks.mapTo(HashSet()) { it.uuid })
        
        return mergedNetwork
    }
    
    private suspend fun <T : Network<T>> reassignNetworks(
        mergedNetwork: ProtoNetwork<T>,
        previous: Set<UUID>
    ) {
        val queue = LinkedList<NetworkNode>()
        val exploredNodes = HashSet<NetworkNode>()
        for ([_, con] in mergedNetwork.nodes) {
            val node = con.node
            queue += node
            exploredNodes += node
        }
        
        val type = mergedNetwork.type
        val now = mergedNetwork.uuid
        
        while (queue.isNotEmpty()) {
            val node = queue.poll()
            
            when (node) {
                is NetworkBridge -> state.getNetworks(node).replaceAll { _, id -> if (id in previous) now else id }
                is NetworkEndPoint -> state.getNetworks(node).replaceAll { _, _, id -> if (id in previous) now else id }
            }
            
            state.forEachConnectedNode(node, type) { _, connectedNode ->
                if (connectedNode !in exploredNodes) {
                    queue += connectedNode
                    exploredNodes += connectedNode
                }
            }
        }
    }
    
    override fun toString(): String {
        return "AddBridgeTask(bridge=$node, supportedNetworkTypes=$supportedNetworkTypes, bridgeFaces=$bridgeFaces)"
    }
    
}