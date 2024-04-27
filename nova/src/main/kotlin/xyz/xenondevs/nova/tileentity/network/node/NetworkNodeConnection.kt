package xyz.xenondevs.nova.tileentity.network.node

import org.bukkit.block.BlockFace
import xyz.xenondevs.commons.collections.enumSet

sealed interface NetworkNodeConnection {
    
    val node: NetworkNode
    
    val faces: Set<BlockFace>
    
    operator fun component1(): NetworkNode = node
    operator fun component2(): Set<BlockFace> = faces
    
}

data class MutableNetworkNodeConnection(
    override val node: NetworkNode, 
    override val faces: MutableSet<BlockFace> = enumSet()
) : NetworkNodeConnection