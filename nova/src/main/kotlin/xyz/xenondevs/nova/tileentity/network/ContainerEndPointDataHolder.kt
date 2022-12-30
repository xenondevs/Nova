package xyz.xenondevs.nova.tileentity.network

import org.bukkit.block.BlockFace

/**
 * An [EndPointDataHolder] that has [EndPointContainers][EndPointContainer] assigned to its block faces.
 * Also has a channel configuration and insert / extract priorities.
 */
interface ContainerEndPointDataHolder <C : EndPointContainer> : EndPointDataHolder {
    
    val allowedConnectionTypes: Map<C, NetworkConnectionType>
    val containerConfig: MutableMap<BlockFace, C>
    val channels: MutableMap<BlockFace, Int>
    val insertPriorities: MutableMap<BlockFace, Int>
    val extractPriorities: MutableMap<BlockFace, Int>
    
    fun isExtract(face: BlockFace): Boolean {
        return NetworkConnectionType.EXTRACT in connectionConfig[face]!!.included
    }
    
    fun isInsert(face: BlockFace): Boolean {
        return NetworkConnectionType.INSERT in connectionConfig[face]!!.included
    }
    
}