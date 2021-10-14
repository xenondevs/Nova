package xyz.xenondevs.nova.tileentity.network.item.holder

import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.tileentity.network.EndPointDataHolder
import xyz.xenondevs.nova.tileentity.network.NetworkManager
import xyz.xenondevs.nova.tileentity.network.item.ItemConnectionType
import xyz.xenondevs.nova.tileentity.network.item.ItemFilter
import xyz.xenondevs.nova.tileentity.network.item.inventory.NetworkedInventory

interface ItemHolder : EndPointDataHolder {
    
    val inventories: MutableMap<BlockFace, NetworkedInventory>
    val itemConfig: MutableMap<BlockFace, ItemConnectionType>
    val insertFilters: MutableMap<BlockFace, ItemFilter>
    val extractFilters: MutableMap<BlockFace, ItemFilter>
    val insertPriorities: MutableMap<BlockFace, Int>
    val extractPriorities: MutableMap<BlockFace, Int>
    val channels: MutableMap<BlockFace, Int>
    val allowedConnectionTypes: Map<NetworkedInventory, ItemConnectionType>
    
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
    
    fun cycleItemConfig(face: BlockFace, plus: Boolean) {
        NetworkManager.handleEndPointRemove(endPoint, true)
        
        val currentConfig = itemConfig[face]!!
        val inventory = inventories[face]!!
        val allowedConfigs = allowedConnectionTypes[inventory]!!.included
        var index = allowedConfigs.indexOf(currentConfig) + if (plus) 1 else -1
        
        if (index >= allowedConfigs.size) index = 0
        else if (index < 0) index = allowedConfigs.lastIndex
        
        itemConfig[face] = allowedConfigs[index]
        
        NetworkManager.handleEndPointAdd(endPoint, false)
        endPoint.updateNearbyBridges()
    }
    
}