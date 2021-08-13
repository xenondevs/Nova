package xyz.xenondevs.nova.tileentity.network

import org.bukkit.Location
import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.util.getNeighboringTileEntitiesOfType
import java.util.*

interface NetworkNode {
    
    /**
     * The location of this [NetworkNode]
     */
    val location: Location
    
    /**
     * Stores the [NetworkNode]s that are connected to this [NetworkNode].
     *
     * For [NetworkBridge]s, connected nodes can be [NetworkBridge]s as well as
     * [NetworkEndPoint]s. For [NetworkEndPoint]s, only [NetworkBridge]s.
     *
     * Should always contain the [NetworkType] keys, but only contain the
     * [BlockFace]s that actually have a [NetworkNode] connected to them.
     */
    val connectedNodes: MutableMap<NetworkType, MutableMap<BlockFace, NetworkNode>>
    
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
    
    fun getNearbyNodes() = location.getNeighboringTileEntitiesOfType<NetworkNode>()
    
    fun getNearbyEndPoints() = location.getNeighboringTileEntitiesOfType<NetworkEndPoint>()
    
    fun getNearbyBridges() =
        location.getNeighboringTileEntitiesOfType<NetworkBridge>().filter { (_, bridge) -> bridge.networks.isNotEmpty() }
    
    fun getNearbyBridges(networkType: NetworkType) =
        getNearbyBridges().filter { (_, bridge) -> bridge.networks[networkType] != null }
    
    fun updateNearbyBridges() =
        getNearbyBridges().forEach { (_, bridge) -> bridge.handleNetworkUpdate() }
    
}