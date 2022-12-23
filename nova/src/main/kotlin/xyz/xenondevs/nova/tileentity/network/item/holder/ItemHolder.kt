package xyz.xenondevs.nova.tileentity.network.item.holder

import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.tileentity.network.ContainerEndPointDataHolder
import xyz.xenondevs.nova.tileentity.network.item.ItemFilter
import xyz.xenondevs.nova.tileentity.network.item.inventory.NetworkedInventory

interface ItemHolder : ContainerEndPointDataHolder<NetworkedInventory> {
    
    /**
     * A [NetworkedInventory] that can be used to access all inventories of this [ItemHolder].
     * Can be null if this [ItemHolder] doesn't support a merged "all" inventory.
     */
    val mergedInventory: NetworkedInventory?
    
    val insertFilters: MutableMap<BlockFace, ItemFilter>
    val extractFilters: MutableMap<BlockFace, ItemFilter>
    
}