package xyz.xenondevs.nova.world.block.tileentity.network.type.item.holder

import xyz.xenondevs.nova.world.*

import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.util.CubeFaceMap
import xyz.xenondevs.nova.world.block.tileentity.network.node.ContainerEndPointDataHolder
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.ItemFilter
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.inventory.NetworkedInventory

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
    var insertFilters: CubeFaceMap<ItemFilter<*>?>
    
    /**
     * Stores the extraction [ItemFilters][ItemFilter] per [BlockFace].
     */
    var extractFilters: CubeFaceMap<ItemFilter<*>?>
    
}
