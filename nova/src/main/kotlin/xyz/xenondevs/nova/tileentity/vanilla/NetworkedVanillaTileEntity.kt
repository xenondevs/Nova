package xyz.xenondevs.nova.tileentity.vanilla

import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.nova.tileentity.network.NetworkManager
import xyz.xenondevs.nova.tileentity.network.node.NetworkEndPoint
import xyz.xenondevs.nova.tileentity.network.node.NetworkNode
import xyz.xenondevs.nova.world.BlockPos

internal abstract class NetworkedVanillaTileEntity internal constructor(
    pos: BlockPos,
    data: Compound
) : VanillaTileEntity(pos, data), NetworkEndPoint {
    
    final override val owner = null
    final override val linkedNodes = HashSet<NetworkNode>()
    
    override fun handlePlace() {
        NetworkManager.queueAddEndPoint(this)
    }
    
    override fun handleBreak() {
        NetworkManager.queueRemoveEndPoint(this)
    }
    
}