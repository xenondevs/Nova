package xyz.xenondevs.nova.tileentity.network

import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.util.emptyEnumMap

interface NetworkEndPoint : NetworkNode {
    
    val networks: MutableMap<NetworkType, MutableMap<BlockFace, Network>>
    val holders: Map<NetworkType, EndPointDataHolder>
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
    
}

