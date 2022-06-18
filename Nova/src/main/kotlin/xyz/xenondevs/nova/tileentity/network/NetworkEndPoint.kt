package xyz.xenondevs.nova.tileentity.network

import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.util.emptyEnumMap
import java.util.*

interface NetworkEndPoint : NetworkNode {
    
    /**
     * Stores the [Networks][Network] that this [NetworkEndPoint] is a part of.
     */
    val networks: MutableMap<NetworkType, MutableMap<BlockFace, Network>>
    
    /**
     * Stores the [EndPointDataHolders][EndPointDataHolder] for the supported [NetworkTypes][NetworkType].
     */
    val holders: Map<NetworkType, EndPointDataHolder>
    
    /**
     * Retrieves the serialized networks from internal storage or null if not present.
     */
    fun retrieveSerializedNetworks(): Map<NetworkType, Map<BlockFace, UUID>>?
    
    /**
     * The [BlockFaces][BlockFace] at which connections are allowed for a specific [NetworkType].
     */
    val allowedFaces: Map<NetworkType, Set<BlockFace>>
        get() = holders.entries.associate { (type, holder) -> type to holder.allowedFaces }
    
    fun getNetworks(): List<Pair<BlockFace, Network>> =
        networks.values.flatMap { networkMap -> networkMap.map { faceMap -> faceMap.key to faceMap.value } }
    
    fun getFaceMap(networkType: NetworkType): MutableMap<BlockFace, Network> {
        return networks.getOrPut(networkType) { emptyEnumMap() }
    }
    
    fun setNetwork(networkType: NetworkType, face: BlockFace, network: Network) {
        getFaceMap(networkType)[face] = network
    }
    
    fun getNetwork(networkType: NetworkType, face: BlockFace) =
        networks[networkType]?.get(face)
    
    fun removeNetwork(networkType: NetworkType, face: BlockFace) {
        getFaceMap(networkType).remove(face)
    }
    
    /**
     * Converts the [networks] map to a serializable version.
     */
    fun serializeNetworks(): Map<NetworkType, Map<BlockFace, UUID>> {
        return networks.entries.associateTo (HashMap()) { entry ->
            entry.key to entry.value.mapValuesTo(emptyEnumMap()) { it.value.uuid }
        }
    }
    
}

