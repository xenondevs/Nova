package xyz.xenondevs.nova.network

import com.google.common.base.Preconditions
import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.util.contentEquals
import xyz.xenondevs.nova.util.enumMapOf
import xyz.xenondevs.nova.util.filterIsInstanceValues
import xyz.xenondevs.nova.util.runTaskTimer

object NetworkManager {
    
    val networks = ArrayList<Network>()
    
    fun init() {
        runTaskTimer(0, 1) {
            networks.removeIf { it.isEmpty() }
            networks.forEach(Network::handleTick)
        }
    }
    
    fun handleEndPointAdd(endPoint: NetworkEndPoint) {
        val bridgesToUpdate = HashSet<NetworkBridge>()
        NetworkType.values().forEach { networkType ->
            val allowedFaces = endPoint.allowedFaces[networkType]
            if (allowedFaces != null) { // does the endpoint want to have any connections?
                // loop over all bridges nearby to possibly connect to
                endPoint.getNearbyBridges(networkType)
                    .filter { (face, bridge) ->
                        allowedFaces.contains(face) // does the endpoint want a connection at that face
                            && bridge.bridgeFaces.contains(face.oppositeFace) // does the bridge want a connection at that face
                    }.forEach { (face, bridge) ->
                        // add to network
                        val network = bridge.networks[networkType]!!
                        endPoint.setNetwork(networkType, face, network)
                        network.addEndPoint(endPoint, face)
                        bridgesToUpdate += bridge
                        
                        // tell the bridge that we connected to it
                        bridge.connectedNodes[networkType]!![face.oppositeFace] = endPoint
                        
                        // remember that we connected to it
                        endPoint.connectedNodes[networkType]!![face] = bridge
                    }
            }
        }
        
        bridgesToUpdate.forEach(NetworkBridge::handleNetworkUpdate)
    }
    
    fun handleBridgeAdd(bridge: NetworkBridge, vararg supportedNetworkTypes: NetworkType) {
        Preconditions.checkArgument(supportedNetworkTypes.isNotEmpty(), "Bridge needs to support at least one network type")
        
        val nearbyNodes: Map<BlockFace, NetworkNode> = bridge.getNearbyNodes()
        val nearbyBridges: Map<BlockFace, NetworkBridge> = nearbyNodes.filterIsInstanceValues()
        val nearbyEndPoints: Map<BlockFace, NetworkEndPoint> = nearbyNodes.filterIsInstanceValues()
        
        supportedNetworkTypes.forEach { networkType ->
            val previousNetworks = HashSet<Network>()
            nearbyBridges.forEach { (face, otherBridge) ->
                if (bridge.bridgeFaces.contains(face)
                    && otherBridge.bridgeFaces.contains(face.oppositeFace)) { // if bridge connects to this bridge
                    
                    // bridges won't have a network if they haven't been fully initialized yet
                    if (otherBridge.networks.containsKey(networkType)) {
                        // a possible network to connect to
                        previousNetworks += otherBridge.networks[networkType]!!
                    }
                    
                    // tell that bridge we connected to it
                    otherBridge.connectedNodes[networkType]!![face.oppositeFace] = bridge
                    
                    // remember that we connected to it
                    bridge.connectedNodes[networkType]!![face] = otherBridge
                }
            }
            
            // depending on how many possible networks there are, perform the required action
            val network = when {
                previousNetworks.size > 1 -> {
                    // MERGE NETWORKS
                    val newNetwork = networkType.networkConstructor()
                    
                    // move nodes from all previous networks to new network
                    previousNetworks.forEach { network ->
                        network.nodes.forEach { node -> node.move(network, newNetwork) }
                        newNetwork.addAll(network)
                    }
                    
                    // remove old networks, add new network
                    networks -= previousNetworks
                    networks += newNetwork
                    
                    newNetwork
                }
                
                previousNetworks.size == 1 -> {
                    // CONNECT TO NETWORK
                    previousNetworks.first()
                }
                
                else -> {
                    // MAKE A NEW NETWORK
                    networkType.networkConstructor().also { networks += it }
                }
            }
            
            // Add the Bridge to the network
            bridge.networks[networkType] = network
            network.addBridge(bridge)
            
            // Connect EndPoints
            nearbyEndPoints.forEach { (face, endPoint) ->
                if (bridge.bridgeFaces.contains(face)) {
                    val allowedFaces = endPoint.allowedFaces[networkType]
                    val oppositeFace = face.oppositeFace
                    if (allowedFaces != null && allowedFaces.contains(oppositeFace)) {
                        
                        // add to network
                        endPoint.setNetwork(networkType, oppositeFace, network)
                        network.addEndPoint(endPoint, oppositeFace)
                        
                        // tell the endpoint that we connected to it
                        endPoint.connectedNodes[networkType]!![oppositeFace] = bridge
                        
                        // remember that we connected to that endpoint
                        bridge.connectedNodes[networkType]!![face] = endPoint
                    }
                }
            }
        }
        
        // update nearby bridges
        bridge.updateNearbyBridges()
        
        // update itself
        bridge.handleNetworkUpdate()
    }
    
    
    fun handleEndPointRemove(endPoint: NetworkEndPoint, unload: Boolean) {
        endPoint.networks.forEach { (_, networkMap) -> networkMap.forEach { (_, network) -> network.removeNode(endPoint) } }
        endPoint.networks.clear()
        NetworkType.values().forEach { endPoint.connectedNodes[it] = enumMapOf() }
        
        // tell all the connected nodes that we no longer exist
        endPoint.connectedNodes.forEach { (networkType, faceMap) ->
            faceMap.forEach { (face, node) ->
                node.connectedNodes[networkType]!!.remove(face.oppositeFace)
            }
        }
        
        if(!unload) endPoint.updateNearbyBridges()
    }
    
    fun handleBridgeRemove(bridge: NetworkBridge, unload: Boolean) {
        bridge.networks.forEach { (networkType, currentNetwork) ->
            // get nodes that are directly connected to this bridge
            val directlyConnected = bridge.connectedNodes[networkType]!!
            
            // disconnect nearby EndPoints
            directlyConnected
                .filter { (_, node) -> node is NetworkEndPoint }
                .forEach { (face, endPoint) ->
                    endPoint as NetworkEndPoint
                    
                    val oppositeFace = face.oppositeFace
                    
                    // there is no longer a network connection at this block face
                    endPoint.removeNetwork(networkType, oppositeFace)
                    endPoint.connectedNodes[networkType]!!.remove(oppositeFace)
                    
                    // remove from network in it's current ConnectionType
                    currentNetwork.removeNode(endPoint)
                    if (endPoint.getFaceMap(networkType).filter { (_, network) -> network == currentNetwork }.isNotEmpty()) {
                        // there are still connections to that EndPoint, but it may not have full functionality anymore
                        endPoint.getFaceMap(networkType).forEach { (face, network) ->
                            if (network == currentNetwork) currentNetwork.addEndPoint(endPoint, face)
                        }
                    }
                }
            
            val connectedBridges: Map<BlockFace, NetworkBridge> = directlyConnected.filterIsInstanceValues()
            
            // remove this bridge from the connectedNodes map of the connected bridges
            connectedBridges.forEach { (face, bridge) ->
                bridge.connectedNodes[networkType]!!.remove(face.oppositeFace)
            }
            
            // if the bridge was connected to multiple other bridges, split networks
            if (connectedBridges.size > 1) {
                // remove previous network from networks
                networks -= currentNetwork
                
                // split attached networks
                val networks = ArrayList<Network>()
                
                for ((_, entrySet) in bridge.getNetworkedNodes(networkType)) {
                    val nodes = entrySet.mapTo(HashSet()) { it.value }
                    
                    // prevent networks with only one EnergyStorage and nothing else
                    if (nodes.size == 1 && nodes.first() !is NetworkBridge) continue
                    
                    // check that the same network doesn't already exist
                    if (networks.none { network -> network.nodes.contentEquals(nodes) }) {
                        val network = networkType.networkConstructor()
                        
                        entrySet.forEach { (face, node) ->
                            if (node is NetworkBridge) {
                                network.addBridge(node)
                                node.networks[networkType] = network
                            } else if (node is NetworkEndPoint) {
                                val oppositeFace = face.oppositeFace
                                network.addEndPoint(node, oppositeFace)
                                node.setNetwork(networkType, oppositeFace, network)
                            }
                        }
                        
                        networks += network
                    }
                }
                
                this.networks += networks
            } else {
                // no need for splitting networks
                currentNetwork.removeNode(bridge)
                bridge.networks.remove(networkType)
            }
        }
    
        NetworkType.values().forEach { bridge.connectedNodes[it] = enumMapOf() }
    
        // update nearby bridges
        if (!unload) bridge.updateNearbyBridges()
    }
    
}