package xyz.xenondevs.nova.tileentity.network.item.holder

import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.tileentity.network.EndPointDataHolder
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.item.ItemFilter
import xyz.xenondevs.nova.tileentity.network.item.inventory.NetworkedInventory

interface ItemHolder : EndPointDataHolder {
    
    val inventories: MutableMap<BlockFace, NetworkedInventory>
    val itemConfig: MutableMap<BlockFace, NetworkConnectionType>
    val insertFilters: MutableMap<BlockFace, ItemFilter>
    val extractFilters: MutableMap<BlockFace, ItemFilter>
    val insertPriorities: MutableMap<BlockFace, Int>
    val extractPriorities: MutableMap<BlockFace, Int>
    val channels: MutableMap<BlockFace, Int>
    val allowedConnectionTypes: Map<NetworkedInventory, NetworkConnectionType>
    
    override val allowedFaces: Set<BlockFace>
        get() = itemConfig.mapNotNullTo(HashSet()) { if (it.value == NetworkConnectionType.NONE) null else it.key }
    
    fun isExtract(face: BlockFace): Boolean {
        return NetworkConnectionType.EXTRACT in itemConfig[face]!!.included
    }
    
    fun isInsert(face: BlockFace): Boolean {
        return NetworkConnectionType.INSERT in itemConfig[face]!!.included
    }
    
}