package xyz.xenondevs.nova.tileentity.network

import org.bukkit.Location
import org.bukkit.block.BlockFace
import xyz.xenondevs.commons.collections.enumMap
import xyz.xenondevs.nova.data.serialization.DataHolder
import xyz.xenondevs.nova.util.getNeighboringTileEntitiesOfType
import java.util.*

sealed interface NetworkNode {
    
    /**
     * The location of this [NetworkNode]
     */
    val location: Location
    
    /**
     * The uuid of this [NetworkNode]
     */
    val uuid: UUID
    
    /**
     * If this [NetworkNode] has been loaded by the [NetworkManager].
     */
    var isNetworkInitialized: Boolean
    
    /**
     * Stores the [NetworkNodes][NetworkNode] that are connected to this [NetworkNode].
     */
    val connectedNodes: MutableMap<NetworkType, MutableMap<BlockFace, NetworkNode>>
    
    /**
     * Sets given the [node] as a connected [NetworkNode] at the given [face] for the specified [networkType].
     */
    fun setConnectedNode(networkType: NetworkType, face: BlockFace, node: NetworkNode) {
        connectedNodes.getOrPut(networkType, ::enumMap)[face] = node
    }
    
    /**
     * Gets the [NetworkNode] that is connected at that [face] under the specified [networkType]
     * or null if there isn't one.
     */
    fun getConnectedNode(networkType: NetworkType, face: BlockFace): NetworkNode? {
        return connectedNodes[networkType]?.get(face)
    }
    
    /**
     * Gets the [NetworkNode] that is connected at [face] under any [NetworkType] or null if there isn't one.
     */
    fun getConnectedNode(face: BlockFace): NetworkNode? {
        return connectedNodes.firstNotNullOfOrNull { it.value[face] }
    }
    
    /**
     * Removes the connected [NetworkNode] for the [networkType] at the [face].
     */
    fun removeConnectedNode(networkType: NetworkType, face: BlockFace) {
        connectedNodes[networkType]?.remove(face)
    }
    
    /**
     * Gets a map of [NetworkType] and [Network] which are attached at this block face.
     */
    fun getNetworks(face: BlockFace): Map<NetworkType, Network> {
        @Suppress("REDUNDANT_ELSE_IN_WHEN") // kotlin compiler bug
        return when (this) {
            is NetworkEndPoint -> {
                val networks = HashMap<NetworkType, Network>()
                this.networks.forEach { (networkType, faceMap) ->
                    faceMap.forEach { (f, network) -> if (f == face) networks[networkType] = network }
                }
                
                networks
            }
            is NetworkBridge -> networks
            else -> throw UnsupportedOperationException()
        }
    }
    
    /**
     * Changes all connections from [previousNetwork] to [newNetwork]
     */
    fun move(previousNetwork: Network, newNetwork: Network) {
        @Suppress("REDUNDANT_ELSE_IN_WHEN") // kotlin compiler bug
        when (this) {
            is NetworkBridge -> networks.replaceAll { _, network -> if (network == previousNetwork) newNetwork else network }
            is NetworkEndPoint -> networks.forEach { (_, networkMap) ->
                networkMap.replaceAll { _, network -> if (network == previousNetwork) newNetwork else network }
            }
            else -> throw UnsupportedOperationException()
        }
    }
    
    /**
     * Retrieves a map of directly adjacent [NetworkNodes][NetworkNode], ignoring additional hitboxes.
     * 
     * The nodes do not need to be in the same network.
     */
    fun getNearbyNodes(): Map<BlockFace, NetworkNode> =
        location.getNeighboringTileEntitiesOfType(false)
    
    /**
     * Retrieves a map of directly adjacent [NetworkEndPoints][NetworkEndPoint], ignoring additional hitboxes.
     * 
     * The end points do not need to be in the same network.
     */
    fun getNearbyEndPoints(): Map<BlockFace, NetworkEndPoint> =
        location.getNeighboringTileEntitiesOfType(false)
    
    /**
     * Retrieves a map of directly adjacent [NetworkBridges][NetworkBridge].
     * 
     * The bridges do not need to be in the same network.
     */
    fun getNearbyBridges(): Map<BlockFace, NetworkBridge> =
        location.getNeighboringTileEntitiesOfType<NetworkBridge>(false).filter { (_, bridge) -> bridge.networks.isNotEmpty() }
    
    /**
     * Retrieves a map of directly adjacent [NetworkBridges][NetworkBridge] supporting the given [networkType].
     * 
     * The bridges do not need to be in the same network.
     */
    fun getNearbyBridges(networkType: NetworkType): Map<BlockFace, NetworkBridge> =
        getNearbyBridges().filter { (_, bridge) -> bridge.networks[networkType] != null }
    
    /**
     * Calls [NetworkBridge.handleNetworkUpdate] for all directly adjacent bridges.
     */
    fun updateNearbyBridges() =
        getNearbyBridges().forEach { (_, bridge) -> bridge.handleNetworkUpdate() }
    
    /**
     * Serializes and writes the [connectedNodes] map to internal storage.
     */
    fun serializeConnectedNodes() {
        require(this is DataHolder)
        
        if (!isNetworkInitialized)
            return
        
        val serializedConnectedNodes = connectedNodes.mapValuesTo(HashMap()) { faceMap ->
            faceMap.value.mapValuesTo(enumMap()) { it.value.uuid }
        }
        
        storeData("connectedNodes", serializedConnectedNodes)
    }
    
    /**
     * Retrieves the serialized connectedNodes map from internal storage or null if not present.
     */
    fun retrieveSerializedConnectedNodes(): Map<NetworkType, Map<BlockFace, UUID>>? {
        require(this is DataHolder)
        return retrieveDataOrNull<HashMap<NetworkType, EnumMap<BlockFace, UUID>>>("connectedNodes")
    }
    
}