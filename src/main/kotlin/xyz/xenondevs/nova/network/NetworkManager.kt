package xyz.xenondevs.nova.network

import xyz.xenondevs.nova.util.contentEquals
import xyz.xenondevs.nova.util.runTaskTimer

object NetworkManager {
    
    private val networks = ArrayList<Network>()
    
    init {
        runTaskTimer(0, 1) {
            networks.removeIf { it.isEmpty() }
            networks.forEach(Network::handleTick)
        }
    }
    
    fun handleEndPointAdd(endPoint: NetworkEndPoint) {
        NetworkType.values().forEach { networkType ->
            val allowedFaces = endPoint.allowedFaces[networkType]
            if (allowedFaces != null) {
                endPoint.getNearbyBridges(networkType)
                    .filter { (face, _) -> allowedFaces.contains(face) }
                    .forEach { (face, bridge) ->
                        val network = bridge.networks[networkType]!!
                        endPoint.setNetwork(networkType, face, network)
                        network.addEndPoint(endPoint, face)
                        bridge.handleNetworkUpdate()
                    }
            }
        }
    }
    
    fun handleBridgeAdd(bridge: NetworkBridge, vararg supportedNetworkTypes: NetworkType) {
        supportedNetworkTypes.forEach { networkType ->
            
            val previousNetworks = HashSet<Network>()
            bridge.getNearbyBridges(networkType).forEach { (face, otherBridge) ->
                if (otherBridge.bridgeFaces.contains(face.oppositeFace)) { // if bridge connects to this bridge
                    previousNetworks += otherBridge.networks[networkType]!!
                }
            }
            
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
            
            // Connect Storages
            bridge.getNearbyEndPoints().forEach { (face, endPoint) ->
                val allowedFaces = endPoint.allowedFaces[networkType]
                val oppositeFace = face.oppositeFace
                if (allowedFaces != null && allowedFaces.contains(oppositeFace)) {
                    endPoint.setNetwork(networkType, oppositeFace, network)
                    network.addEndPoint(endPoint, oppositeFace)
                }
            }
        }
        
        // update nearby bridges
        bridge.updateNearbyBridges()
    }
    
    
    fun handleEndPointRemove(endPoint: NetworkEndPoint, unload: Boolean) {
        endPoint.networks.forEach { (_, networkMap) -> networkMap.forEach { (_, network) -> network.removeNode(endPoint) } }
        endPoint.networks.clear()
        
        if (!unload) endPoint.updateNearbyBridges()
    }
    
    fun handleBridgeRemove(bridge: NetworkBridge, unload: Boolean) {
        bridge.networks.forEach { (networkType, currentNetwork) ->
            // get nodes that are directly connected to this bridge
            val directlyConnected = bridge.connectedNodes[networkType] ?: emptyMap()
            
            // disconnect nearby EndPoints
            directlyConnected
                .filter { (_, node) -> node is NetworkEndPoint }
                .forEach { (face, endPoint) ->
                    endPoint as NetworkEndPoint
                    
                    // there is no longer a network connection at this block face
                    endPoint.removeNetwork(networkType, face.oppositeFace)
                    
                    // remove from network in it's current ConnectionType
                    currentNetwork.removeNode(endPoint)
                    if (endPoint.getFaceMap(networkType).filter { (_, network) -> network == currentNetwork }.isNotEmpty()) {
                        // there are still connections to that EndPoint, but it may not have full functionality anymore
                        endPoint.getFaceMap(networkType).forEach { (face, network) ->
                            if (network == currentNetwork) currentNetwork.addEndPoint(endPoint, face)
                        }
                    }
                }
            
            // if the bridge was connected to multiple other bridges, split networks
            if (directlyConnected.filter { (_, node) -> node is NetworkBridge }.count() > 1) {
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
        
        // update nearby bridges
        if (!unload) bridge.updateNearbyBridges()
    }
    
}