package xyz.xenondevs.nova.tileentity.network.type.item.holder

import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.tileentity.network.node.ContainerEndPointDataHolder
import xyz.xenondevs.nova.tileentity.network.type.item.ItemFilter
import xyz.xenondevs.nova.tileentity.network.type.item.inventory.NetworkedInventory

/**
 * End point data holder for nova:item networks.
 */
interface ItemHolder : ContainerEndPointDataHolder<NetworkedInventory> {
    
    /**
     * A [NetworkedInventory] that can be used to access all inventories of this [ItemHolder].
     * Can be null if this [ItemHolder] doesn't support a merged "all" inventory.
     */
    val mergedInventory: NetworkedInventory?
    
    /**
     * Stores the insertion [ItemFilters][ItemFilter] per [BlockFace].
     */
    val insertFilters: MutableMap<BlockFace, ItemFilter>
    
    /**
     * Stores the extraction [ItemFilters][ItemFilter] per [BlockFace].
     */
    val extractFilters: MutableMap<BlockFace, ItemFilter>
    
}