package xyz.xenondevs.nova.network

import org.bukkit.Location
import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.util.getNeighboringTileEntitiesOfType
import java.util.*

fun Location.getNearbyNodes() = getNeighboringTileEntitiesOfType<NetworkNode>()

interface NetworkNode {
    
    val location: Location
    
    fun getNetworks(face: BlockFace): Map<NetworkType, Network> {
        return if (this is NetworkBridge) networks
        else {
            this as NetworkEndPoint
            
            val networks = EnumMap<NetworkType, Network>(NetworkType::class.java)
            this.networks.forEach { (networkType, faceMap) ->
                faceMap.forEach { (f, network) -> if (f == face) networks[networkType] = network }
            }
            
            networks
        }
    }
    
    fun move(previousNetwork: Network, newNetwork: Network) {
        if (this is NetworkBridge) {
            networks.replaceAll { _, network -> if (network == previousNetwork) newNetwork else network }
        } else if (this is NetworkEndPoint) {
            networks.forEach { (_, networkMap) ->
                networkMap.replaceAll { _, network -> if (network == previousNetwork) newNetwork else network }
            }
        }
    }
    
    fun getNearbyNodes() = location.getNearbyNodes()
    
    fun getNearbyEndPoints() = location.getNeighboringTileEntitiesOfType<NetworkEndPoint>()
    
    fun getNearbyBridges() =
        location.getNeighboringTileEntitiesOfType<NetworkBridge>().filter { (_, bridge) -> bridge.networks.isNotEmpty() }
    
    fun getNearbyBridges(networkType: NetworkType) =
        getNearbyBridges().filter { (_, bridge) -> bridge.networks[networkType] != null }
    
    fun updateNearbyBridges() =
        getNearbyBridges().forEach { (_, bridge) -> bridge.handleNetworkUpdate() }
    
}