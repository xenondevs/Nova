package xyz.xenondevs.nova.energy

import org.bukkit.Location
import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.util.getNeighboringTileEntitiesOfType

fun EnergyNode.getNetwork(face: BlockFace): EnergyNetwork? {
    return if (this is EnergyBridge) network
    else (this as EnergyStorage).networks[face]
}

fun EnergyNode.setNetworkIfAllowed(face: BlockFace, network: EnergyNetwork) {
    if (this is EnergyBridge) this.network = network
    else if (this is EnergyStorage && configuration[face]!! != EnergyConnectionType.NONE) networks[face] = network
}

fun EnergyNode.move(previousNetwork: EnergyNetwork, newNetwork: EnergyNetwork) {
    if (this is EnergyBridge) {
        if (network == previousNetwork) network = newNetwork
    } else if (this is EnergyStorage) {
        networks.replaceAll { _, network -> if (network == previousNetwork) newNetwork else network }
    }
}

fun Location.getNearbyNodes() = getNeighboringTileEntitiesOfType<EnergyNode>()

fun EnergyNode.getNearbyNodes() = location.getNearbyNodes()

fun EnergyNode.getNearbyBridges() = location.getNeighboringTileEntitiesOfType<EnergyBridge>().filter { it.value.network != null }

fun EnergyNode.getNearbyStorages() = location.getNeighboringTileEntitiesOfType<EnergyStorage>()

fun EnergyNode.updateNearbyBridges() = getNearbyBridges().forEach { (_, bridge) -> bridge.handleNetworkUpdate() }

interface EnergyNode {
    
    val location: Location
    
}