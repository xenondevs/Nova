package xyz.xenondevs.nova.tileentity.vanilla

import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.data.world.block.state.VanillaTileEntityState
import xyz.xenondevs.nova.tileentity.network.Network
import xyz.xenondevs.nova.tileentity.network.NetworkEndPoint
import xyz.xenondevs.nova.tileentity.network.NetworkManager
import xyz.xenondevs.nova.tileentity.network.NetworkNode
import xyz.xenondevs.nova.tileentity.network.NetworkType
import xyz.xenondevs.nova.util.data.HashUtils

internal abstract class NetworkedVanillaTileEntity internal constructor(state: VanillaTileEntityState) : VanillaTileEntity(state), NetworkEndPoint {
    
    final override val location = blockState.pos.location
    final override val uuid = HashUtils.getUUID(blockState.pos)
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