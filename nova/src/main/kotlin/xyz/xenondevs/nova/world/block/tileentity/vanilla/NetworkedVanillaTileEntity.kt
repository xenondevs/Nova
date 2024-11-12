package xyz.xenondevs.nova.world.block.tileentity.vanilla

import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.tileentity.network.NetworkManager
import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkEndPoint
import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkNode

internal abstract class NetworkedVanillaTileEntity internal constructor(
    type: Type,
    pos: BlockPos,
    data: Compound
) : VanillaTileEntity(type, pos, data), NetworkEndPoint {
    
    @Volatile
    final override var isValid = false
    
    final override val owner = null
    final override val linkedNodes = HashSet<NetworkNode>()
    
    override fun handleEnable() {
        // legacy conversion
        if (hasData("connectedNodes") || hasData("networks")) {
            removeData("connectedNodes")
            removeData("networks")
            
            NetworkManager.queueAddEndPoint(this)
        }
        
        isValid = true
    }
    
    override fun handleDisable() {
        isValid = false
    }
    
    override fun handlePlace() {
        NetworkManager.queueAddEndPoint(this)
    }
    
    override fun handleBreak() {
        NetworkManager.queueRemoveEndPoint(this)
        isValid = false
    }
    
}