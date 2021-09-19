package xyz.xenondevs.nova.tileentity.network.item.holder

import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.tileentity.network.EndPointDataHolder
import xyz.xenondevs.nova.tileentity.network.item.ItemConnectionType
import xyz.xenondevs.nova.tileentity.network.item.inventory.NetworkedInventory

interface ItemHolder : EndPointDataHolder {
    
    val inventories: MutableMap<BlockFace, NetworkedInventory>
    val itemConfig: MutableMap<BlockFace, ItemConnectionType>
    
    override val allowedFaces: Set<BlockFace>
        get() = itemConfig.mapNotNullTo(HashSet()) { if (it.value == ItemConnectionType.NONE) null else it.key }
    
    fun setInsert(face: BlockFace, state: Boolean) {
        val type = itemConfig[face]!!
        itemConfig[face] = ItemConnectionType.of(state, type.extract)
    }
    
    fun setExtract(face: BlockFace, state: Boolean) {
        val type = itemConfig[face]!!
        itemConfig[face] = ItemConnectionType.of(type.insert, state)
    }
    
}