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
        val previousNetworks = ArrayList<EnergyNetwork>()
        bridge.getNearbyBridges().forEach { (face, otherBridge) ->
            if (otherBridge.bridgeFaces.contains(face.oppositeFace)) { // if bridge connects to this bridge
                previousNetworks += otherBridge.network!!
            }
        }
        
        // create new network and add new bridge to it
        val newNetwork = EnergyNetwork()
        newNetwork.addBridge(bridge)
        bridge.network = newNetwork
        
        // move nodes from previous network to new network
        previousNetworks.forEach { network ->
            network.getNodes().forEach { node -> node.move(network, newNetwork) }
            newNetwork.addAll(network)
        }
        
        // remove old networks, add new network
        networks -= previousNetworks
        networks += newNetwork
        
        // Connect Storages
        bridge.getNearbyStorages().forEach { (face, storage) ->
            val oppositeFace = face.oppositeFace
            val connectionType = storage.configuration[oppositeFace]!!
            if (connectionType != EnergyConnectionType.NONE) {
                storage.networks[oppositeFace] = newNetwork
                newNetwork.addStorage(storage, connectionType)
            }
        }
        
        // update nearby bridges
        bridge.updateNearbyBridges()
    }
    
    fun handleStorageRemove(storage: EnergyStorage, unload: Boolean) {
        storage.networks.forEach { (_, network) -> network -= storage }
        if (!unload) storage.updateNearbyBridges()
    }
    
    // TODO: optimize
    fun handleBridgeRemove(bridge: EnergyBridge, unload: Boolean) {
        val previousNetwork = bridge.network!!
        
        // disconnect nearby consumers and providers
        bridge.getConnectedNodes()
            .filter { (_, node) -> node is EnergyStorage }
            .forEach { (face, node) ->
                node as EnergyStorage
                
                // there is no longer a network connection at this block face
                node.networks.remove(face.oppositeFace)
                
                // if there is no other connection to this network, remove the node from the network
                if (node.networks.isEmpty()) previousNetwork -= node
            }
        
        // TODO: if connected bridges size == 1, no reason for split calculation
        
        // remove previous network from networks
        networks -= previousNetwork
        
        // split attached networks
        val networks = ArrayList<EnergyNetwork>()
        
        bridge.getNetworkedNodes().forEach { (_, nodesSet) ->
            val nodes = nodesSet.map { it.value }
            // check that the same network doesn't already exist
            if (networks.none { network -> network.getNodes().contentEquals(nodes) }) {
                val network = EnergyNetwork()
                
                nodesSet.forEach { (face, node) ->
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
        
        // update nearby bridges
        if (!unload) bridge.updateNearbyBridges()
    }
    
}