package xyz.xenondevs.nova.energy

import xyz.xenondevs.nova.util.contentEquals
import xyz.xenondevs.nova.util.runTaskTimer

/**
 * Manages all [EnergyNetwork]s
 */
object EnergyNetworkManager {
    
    private val networks = ArrayList<EnergyNetwork>()
    
    init {
        runTaskTimer(0, 1) {
            networks.removeIf { it.isEmpty() }
            networks.forEach(EnergyNetwork::handleTick)
        }
    }
    
    fun handleStorageAdd(storage: EnergyStorage) {
        val allowedFaces = storage.configuration.filter { (_, type) -> type != EnergyConnectionType.NONE }
        storage.getNearbyBridges().forEach { (face, bridge) ->
            if (allowedFaces.contains(face)) {
                val network = bridge.network!!
                storage.networks[face] = bridge.network!!
                network.addStorage(storage, storage.configuration[face]!!)
                bridge.handleNetworkUpdate()
            }
        }
    }
    
    fun handleBridgeAdd(bridge: EnergyBridge) {
        val previousNetworks = HashSet<EnergyNetwork>()
        bridge.getNearbyBridges().forEach { (face, otherBridge) ->
            if (otherBridge.bridgeFaces.contains(face.oppositeFace)) { // if bridge connects to this bridge
                previousNetworks += otherBridge.network!!
            }
        }
        
        val network = when {
            previousNetworks.size > 1 -> {
                // MERGE NETWORKS
                val newNetwork = EnergyNetwork()
                
                // move nodes from all previous network to new network
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
                EnergyNetwork().also { networks += it }
            }
        }
        
        // Add the Bridge to the network
        bridge.network = network
        network.addBridge(bridge)
        
        // Connect Storages
        bridge.getNearbyStorages().forEach { (face, storage) ->
            val oppositeFace = face.oppositeFace
            val connectionType = storage.configuration[oppositeFace]!!
            if (connectionType != EnergyConnectionType.NONE) {
                storage.networks[oppositeFace] = network
                network.addStorage(storage, connectionType)
            }
        }
        
        // update nearby bridges
        bridge.updateNearbyBridges()
    }
    
    fun handleStorageRemove(storage: EnergyStorage, unload: Boolean) {
        storage.networks.forEach { (_, network) -> network -= storage }
        storage.networks.clear()
        if (!unload) storage.updateNearbyBridges()
    }
    
    fun handleBridgeRemove(bridge: EnergyBridge, unload: Boolean) {
        val currentNetwork = bridge.network!!
        
        // get nodes that are directly connected to this bridge
        val directlyConnected = bridge.connectedNodes
        
        // disconnect nearby EnergyStorages
        directlyConnected
            .filter { (_, node) -> node is EnergyStorage }
            .forEach { (face, node) ->
                node as EnergyStorage
                
                // there is no longer a network connection at this block face
                node.networks.remove(face.oppositeFace)
                
                // remove from network in it's current ConnectionType
                currentNetwork -= node
                if (node.networks.filter { (_, network) -> network == currentNetwork }.isNotEmpty()) {
                    // there are still connections to that EnergyStorage, but it may not have full functionality anymore
                    node.networks.forEach { (face, network) ->
                        if (network == currentNetwork)
                            currentNetwork.addStorage(node, node.configuration[face]!!)
                    }
                }
            }
        
        // if the bridge was connected to multiple other bridges, split networks
        if (directlyConnected.filter { (_, node) -> node is EnergyBridge }.count() > 1) {
            // remove previous network from networks
            networks -= currentNetwork
            
            // split attached networks
            val networks = ArrayList<EnergyNetwork>()
            
            for ((_, entrySet) in bridge.getNetworkedNodes()) {
                val nodes = entrySet.mapTo(HashSet()) { it.value }
                
                // prevent networks with only one EnergyStorage and nothing else
                if (nodes.size == 1 && nodes.first() !is EnergyBridge) continue
                
                // check that the same network doesn't already exist
                if (networks.none { network -> network.nodes.contentEquals(nodes) }) {
                    val network = EnergyNetwork()
                    
                    entrySet.forEach { (face, node) ->
                        if (node is EnergyBridge) {
                            network.addBridge(node)
                            node.network = network
                        } else if (node is EnergyStorage) {
                            val oppositeFace = face.oppositeFace
                            network.addStorage(node, node.configuration[oppositeFace]!!)
                            node.networks[oppositeFace] = network
                        }
                    }
                    
                    networks += network
                }
            }
            
            this.networks += networks
        } else {
            // no need for splitting networks
            currentNetwork.removeNode(bridge)
        }
        
        // update nearby bridges
        if (!unload) bridge.updateNearbyBridges()
    }
    
}