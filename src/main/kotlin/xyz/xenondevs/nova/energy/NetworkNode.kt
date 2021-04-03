package xyz.xenondevs.nova.energy

import org.bukkit.Location
import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.util.getNeighboringTileEntitiesOfType

fun NetworkNode.getNetwork(face: BlockFace): EnergyNetwork? {
    return if (this is NetworkEndPoint) {
        if (this is EnergyProvider) provideNetworks[face]
        else (this as EnergyConsumer).consumeNetworks[face]
    } else (this as EnergyBridge).network
}

fun NetworkNode.setNetworkIfAllowed(face: BlockFace, network: EnergyNetwork) {
    if (this is NetworkEndPoint) {
        if (this is EnergyProvider && provideNetworks.containsKey(face)) provideNetworks[face] = network
        else if (this is EnergyConsumer && consumeNetworks.containsKey(face)) consumeNetworks[face] = network
    } else (this as EnergyBridge).network = network
}

fun NetworkNode.getNetworks(): List<EnergyNetwork> {
    val networks = ArrayList<EnergyNetwork>()
    if (this is EnergyBridge) networks += network!!
    if (this is EnergyProvider) networks += provideNetworks.mapNotNull { it.value }
    if (this is EnergyConsumer) networks += consumeNetworks.mapNotNull { it.value }
    
    return networks
}

fun NetworkNode.move(previousNetwork: EnergyNetwork, newNetwork: EnergyNetwork) {
    if (this is EnergyBridge) {
        if (network == previousNetwork) network = newNetwork
    } else {
        if (this is EnergyProvider)
            provideNetworks.replaceAll { _, network -> if (network == previousNetwork) newNetwork else network }
        if (this is EnergyConsumer)
            consumeNetworks.replaceAll { _, network -> if (network == previousNetwork) newNetwork else network }
    }
}

fun Location.getNearbyNodes() = getNeighboringTileEntitiesOfType<NetworkNode>()

fun NetworkNode.getNearbyNodes() = location.getNearbyNodes()

fun NetworkNode.getNearbyBridges() = location.getNeighboringTileEntitiesOfType<EnergyBridge>().filter { it.value.network != null }

fun NetworkNode.getNearbyEndPoints() = location.getNeighboringTileEntitiesOfType<NetworkEndPoint>()

fun NetworkNode.updateNearbyBridges() = getNearbyBridges().forEach { (_, bridge) -> bridge.handleNetworkUpdate() }

interface NetworkNode {
    
    val location: Location
    
}