package xyz.xenondevs.nova.tileentity.network

import org.bukkit.block.BlockFace
import java.util.*

/**
 * Basically cables.
 */
interface NetworkBridge : NetworkNode {
    
    /**
     * The [Network]s this [NetworkBridge] is connected to.
     */
    val networks: MutableMap<NetworkType, Network>
    
    /**
     * Block faces that are allowed to transmit in a [Network].
     */
    val bridgeFaces: Set<BlockFace>
    
    /**
     * A set of [NetworkTypes][NetworkType] that are supported by this bridge.
     */
    val supportedNetworkTypes: Set<NetworkType>
    
    /**
     * An identifier to prevent bridges of different tiers from connecting to each other.
     */
    val typeId: String
    
    /**
     * Called when a network update occurs. This includes [NetworkNodes][NetworkNode] being
     * placed or broken next to this [NetworkBridge] as well as the placement of this bridge itself.
     * 
     * This method is not called when adjacent [NetworkNodes][NetworkNode] are loaded / unloaded.
     */
    fun handleNetworkUpdate() // TODO: Should be in NetworkNode
    
    /**
     * Retrieves the serialized networks map from internal storage.
     */
    fun retrieveSerializedNetworks(): Map<NetworkType, UUID>?
    
    fun setNetwork(networkType: NetworkType, network: Network) {
        networks[networkType] = network
    }
    
    /**
     * Checks if this bridge is able to connect to its neighboring bridge.
     */
    fun canConnect(other: NetworkBridge, requestedType: NetworkType, face: BlockFace): Boolean {
        return typeId == other.typeId
            && bridgeFaces.contains(face)
            && other.bridgeFaces.contains(face.oppositeFace)
            && other.supportedNetworkTypes.contains(requestedType)
    }
    
    /**
     * Checks if this bridge is able to connect to its neighboring end point.
     */
    fun canConnect(other: NetworkEndPoint, requestedType: NetworkType, face: BlockFace): Boolean {
        return bridgeFaces.contains(face) && other.allowedFaces[requestedType]?.contains(face.oppositeFace) ?: false
    }
    
    /**
     * Converts the [networks] map to a serializable version.
     */
    fun serializeNetworks(): HashMap<NetworkType, UUID> =
        networks.entries.associateTo(HashMap()) { it.key to it.value.uuid }
    
}