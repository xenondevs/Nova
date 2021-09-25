package xyz.xenondevs.nova.tileentity.network

import com.google.common.base.Preconditions
import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.tileentity.network.item.ItemNetwork
import xyz.xenondevs.nova.tileentity.vanilla.VanillaTileEntity
import xyz.xenondevs.nova.util.*

object NetworkManager {
    
    val LOCK = ObservableLock()
    val networks = ArrayList<Network>()
    
    fun init() {
        LOGGER.info("Initializing NetworkManager")
        
        runTaskTimer(0, 1) {
            LOCK.tryLockAndRun {
                networks.forEach { network ->
                    if (network is ItemNetwork) {
                        if (serverTick % 20 == 0) network.handleTick()
                    } else network.handleTick()
                }
            }
        }
    }
    
    fun handleEndPointAdd(endPoint: NetworkEndPoint) {
        LOCK.lockAndRun {
            val bridgesToUpdate = HashSet<NetworkBridge>()
            NetworkType.values().forEach { networkType ->
                val allowedFaces = endPoint.allowedFaces[networkType]
                if (allowedFaces != null) { // does the endpoint want to have any connections?
                    // loop over all bridges nearby to possibly connect to
                    endPoint.getNearbyNodes()
                        .forEach endPoints@{ (face, neighborNode) ->
                            val oppositeFace = face.oppositeFace
                            
                            // does the endpoint want a connection at that face?
                            if (!allowedFaces.contains(face)) return@endPoints
                            
                            if (neighborNode is NetworkBridge) {
                                if (neighborNode.networks[networkType] != null // is the bridge in a network (should always be true)
                                    && neighborNode.bridgeFaces.contains(oppositeFace)) { // does the bridge want a connection at that face
                                    
                                    // add to network
                                    val network = neighborNode.networks[networkType]!!
                                    endPoint.setNetwork(networkType, face, network)
                                    network.addEndPoint(endPoint, face)
                                    bridgesToUpdate += neighborNode
                                    
                                    // tell the bridge that we connected to it
                                    neighborNode.connectedNodes[networkType]!![oppositeFace] = endPoint
                                    
                                    // remember that we connected to it
                                    endPoint.connectedNodes[networkType]!![face] = neighborNode
                                }
                            } else if (neighborNode is NetworkEndPoint) {
                                // do not allow a network between two vanilla tile entities
                                if (neighborNode is VanillaTileEntity && endPoint is VanillaTileEntity) return@endPoints
                                
                                if (neighborNode.allowedFaces[networkType]?.contains(oppositeFace) == true // does the endpoint want a connection of this type at that face
                                    && neighborNode.connectedNodes[networkType]!![oppositeFace] == null // does not already connect to something there
                                ) {
                                    
                                    // create a new "local" network
                                    val network = networkType.networkConstructor()
                                    network.addEndPoint(endPoint, face)
                                    network.addEndPoint(neighborNode, face.oppositeFace)
                                    
                                    // would this network make sense? (i.e. no networks of only consumers or only providers)
                                    if (network.isValid()) {
                                        // tell the neighbor that is now connected to this endPoint over the network at that face
                                        neighborNode.connectedNodes[networkType]!![face.oppositeFace] = endPoint
                                        neighborNode.setNetwork(networkType, oppositeFace, network)
                                        
                                        // remember that we're now connected to that node over the network at that face
                                        endPoint.connectedNodes[networkType]!![face] = neighborNode
                                        endPoint.setNetwork(networkType, face, network)
                                        
                                        // add the network
                                        networks += network
                                    }
                                }
                            }
                        }
                }
            }
            
            bridgesToUpdate.forEach(NetworkBridge::handleNetworkUpdate)
        }
    }
    
    fun handleBridgeAdd(bridge: NetworkBridge, vararg supportedNetworkTypes: NetworkType) {
        LOCK.lockAndRun {
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
    }
    
    fun handleEndPointRemove(endPoint: NetworkEndPoint, unload: Boolean) {
        LOCK.lockAndRun {
            // tell all the connected nodes that we no longer exist
            endPoint.connectedNodes.forEach { (networkType, faceMap) ->
                faceMap.forEach { (face, node) ->
                    node.connectedNodes[networkType]!!.remove(face.oppositeFace)
                }
            }
            
            endPoint.networks.forEach { (_, networkMap) ->
                networkMap.forEach { (_, network) ->
                    network.removeNode(endPoint)
                    
                    // remove the network from networks if it isn't valid
                    if (!network.isValid()) networks -= network
                }
            }
            endPoint.networks.clear()
            NetworkType.values().forEach { endPoint.connectedNodes[it] = enumMapOf() }
            
            if (!unload) endPoint.updateNearbyBridges()
        }
    }
    
    fun handleBridgeRemove(bridge: NetworkBridge, unload: Boolean) {
        LOCK.lockAndRun {
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
                    
                    NetworkManager.networks += networks
                } else {
                    // no need for splitting networks
                    currentNetwork.removeNode(bridge)
                    bridge.networks.remove(networkType)
                    
                    // remove the network from networks if it isn't valid
                    if (!currentNetwork.isValid()) networks -= currentNetwork
                }
            }
            
            NetworkType.values().forEach { bridge.connectedNodes[it] = enumMapOf() }
            
            // update nearby bridges
            if (!unload) bridge.updateNearbyBridges()
        }
    }
    
}