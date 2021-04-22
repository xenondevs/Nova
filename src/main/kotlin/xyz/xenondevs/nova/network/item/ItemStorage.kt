package xyz.xenondevs.nova.network.item

import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.network.NetworkEndPoint
import xyz.xenondevs.nova.network.item.inventory.NetworkedInventory

interface ItemStorage : NetworkEndPoint {
    
    val inventories: MutableMap<BlockFace, NetworkedInventory>
    val itemConfig: MutableMap<BlockFace, ItemConnectionType>
    
    fun setInsert(face: BlockFace, state: Boolean) {
        val type = itemConfig[face]!!
        itemConfig[face] = ItemConnectionType.of(state, type.extract)
    }
    
    fun setExtract(face: BlockFace, state: Boolean) {
        val type = itemConfig[face]!!
        itemConfig[face] = ItemConnectionType.of(type.insert, state)
    }
    
}

enum class ItemConnectionType(val insert: Boolean, val extract: Boolean) {
    
    NONE(false, false),
    INSERT(true, false),
    EXTRACT(false, true),
    BUFFER(true, true);
    
    companion object {
        
        fun of(insert: Boolean, extract: Boolean) = values().first { it.insert == insert && it.extract == extract }
        
    }
    
}