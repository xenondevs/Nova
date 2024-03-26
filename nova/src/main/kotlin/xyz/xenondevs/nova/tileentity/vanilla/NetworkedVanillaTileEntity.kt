package xyz.xenondevs.nova.tileentity.vanilla

import org.bukkit.block.BlockFace
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.nova.tileentity.network.Network
import xyz.xenondevs.nova.tileentity.network.NetworkEndPoint
import xyz.xenondevs.nova.tileentity.network.NetworkManager
import xyz.xenondevs.nova.tileentity.network.NetworkNode
import xyz.xenondevs.nova.tileentity.network.NetworkType
import xyz.xenondevs.nova.util.data.HashUtils
import xyz.xenondevs.nova.world.BlockPos

internal abstract class NetworkedVanillaTileEntity internal constructor(
    pos: BlockPos,
    data: Compound
) : VanillaTileEntity(pos, data), NetworkEndPoint {
    
    final override val location = pos.location
    final override val uuid = HashUtils.getUUID(pos)
    final override var isNetworkInitialized = false
    
    final override val networks: MutableMap<NetworkType, MutableMap<BlockFace, Network>> = HashMap()
    final override val connectedNodes: MutableMap<NetworkType, MutableMap<BlockFace, NetworkNode>> = HashMap()
    
    override fun handleInitialized() {
        NetworkManager.queueAsync { it.addEndPoint(this) }
    }
    
    override fun handleRemoved(unload: Boolean) {
        if (!unload) NetworkManager.queueAsync { it.removeEndPoint(this) }
    }
    
    override fun saveData() {
        super.saveData()
        
        serializeNetworks()
        serializeConnectedNodes()
    }
    
}