package xyz.xenondevs.nova.tileentity.network.fluid.holder

import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.tileentity.network.EndPointDataHolder
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.fluid.container.FluidContainer

interface FluidHolder : EndPointDataHolder {
    
    val allowedConnectionTypes: Map<FluidContainer, NetworkConnectionType>
    val containerConfig: MutableMap<BlockFace, FluidContainer>
    val connectionConfig: MutableMap<BlockFace, NetworkConnectionType>
    val channels: MutableMap<BlockFace, Int>
    val insertPriorities: MutableMap<BlockFace, Int>
    val extractPriorities: MutableMap<BlockFace, Int>
    
    override val allowedFaces: Set<BlockFace>
        get() = connectionConfig.mapNotNullTo(HashSet()) { if (it.value == NetworkConnectionType.NONE) null else it.key }
    
    fun isExtract(face: BlockFace): Boolean {
        return NetworkConnectionType.EXTRACT in connectionConfig[face]!!.included
    }
    
    fun isInsert(face: BlockFace): Boolean {
        return NetworkConnectionType.INSERT in connectionConfig[face]!!.included
    }
    
}