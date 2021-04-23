package xyz.xenondevs.nova.network

import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.network.energy.EnergyStorage
import xyz.xenondevs.nova.network.item.ItemStorage
import java.util.*

interface NetworkEndPoint : NetworkNode {
    
    val networks: MutableMap<NetworkType, MutableMap<BlockFace, Network>>
    val allowedFaces: Map<NetworkType, List<BlockFace>>
        get() {
            val map = HashMap<NetworkType, List<BlockFace>>()
            if (this is EnergyStorage)
                map[NetworkType.ENERGY] = this.energyConfig.keys.toList()
            if (this is ItemStorage)
                map[NetworkType.ITEMS] = this.itemConfig.keys.toList()
            
            return map
        }
    
    fun getNetworks(): List<Pair<BlockFace, Network>> =
        networks.values.flatMap { networkMap -> networkMap.map { faceMap -> faceMap.key to faceMap.value } }
    
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

