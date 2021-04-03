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
    
    fun handleProviderAdd(provider: EnergyProvider) {
        val allowedFaces = provider.provideNetworks.keys
        provider.getNearbyBridges().forEach { (face, bridge) ->
            if (allowedFaces.contains(face)) {
                val network = bridge.network!!
                provider.provideNetworks[face] = bridge.network
                network += provider
                bridge.handleNetworkUpdate()
            }
        }
    }
    
    fun handleConsumerAdd(consumer: EnergyConsumer) {
        val allowedFaces = consumer.consumeNetworks.keys
        consumer.getNearbyBridges().forEach { (face, bridge) ->
            if (allowedFaces.contains(face)) {
                val network = bridge.network!!
                consumer.consumeNetworks[face] = bridge.network
                network += consumer
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
        newNetwork += bridge
        bridge.network = newNetwork
        
        // move nodes from previous network to new network
        previousNetworks.forEach { network ->
            network.getNodes().forEach { node ->
                node.move(network, newNetwork)
                newNetwork += node
            }
        }
        
        // remove old networks, add new network
        networks -= previousNetworks
        networks += newNetwork
        
        // Connect EndPoints
        bridge.getNearbyEndPoints().forEach { (face, endPoint) ->
            endPoint.setNetworkIfAllowed(face.oppositeFace, newNetwork)
            newNetwork += endPoint
        }
        
        // update nearby bridges
        bridge.updateNearbyBridges()
    }
    
    fun handleProviderRemove(provider: EnergyProvider, unload: Boolean) {
        provider.provideNetworks.forEach { (_, network) -> if (network != null) network -= provider }
        if (!unload) provider.updateNearbyBridges()
    }
    
    fun handleConsumerRemove(consumer: EnergyConsumer, unload: Boolean) {
        consumer.consumeNetworks.forEach { (_, network) -> if (network != null) network -= consumer }
        if (!unload) consumer.updateNearbyBridges()
    }
    
    // TODO: optimize
    fun handleBridgeRemove(bridge: EnergyBridge, unload: Boolean) {
        val previousNetwork = bridge.network!!
        
        // disconnect nearby consumers and providers
        bridge.getConnectedNodes()
            .filter { (_, node) -> node is NetworkEndPoint }
            .forEach { (face, node) ->
                node as NetworkEndPoint
                
                // there is no longer a network connection at this block face
                node.removeNetwork(face.oppositeFace)
                
                // if there is no other connection to this network, remove the node from the network
                if (!node.getNetworks().contains(previousNetwork)) previousNetwork -= node
            }
        
        // remove previous network from networks
        networks -= previousNetwork
        
        // split attached networks
        val networks = ArrayList<EnergyNetwork>()
        
        bridge.getNetworkedNodes().forEach { (_, nodesSet) ->
            val nodes = nodesSet.map { it.value }
            // check that the same network doesn't already exist
            if (networks.none { network -> network.getNodes().contentEquals(nodes) }) {
                val network = EnergyNetwork(nodesSet.map { it.value })
                nodesSet.forEach { (face, node) -> node.setNetworkIfAllowed(face.oppositeFace, network) }
                
                networks += network
            }
        }
        
        this.networks += networks
        
        // update nearby bridges
        if (!unload) bridge.updateNearbyBridges()
    }
    
}