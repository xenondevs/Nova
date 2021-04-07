package xyz.xenondevs.nova.network

import org.bukkit.block.BlockFace
import java.util.*

interface NetworkEndPoint : NetworkNode {
    
    val networks: MutableMap<NetworkType, MutableMap<BlockFace, Network>>
    val allowedFaces: Map<NetworkType, List<BlockFace>>
    
    fun getFaceMap(networkType: NetworkType): MutableMap<BlockFace, Network> {
        return networks[networkType]
            ?: EnumMap<BlockFace, Network>(BlockFace::class.java).also { networks[networkType] = it }
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

