package xyz.xenondevs.nova.tileentity.network.item

import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.tileentity.network.NetworkBridge

interface ItemBridge : NetworkBridge {
    
    /**
     * How many items can be transferred per tick. If there are different
     * types of [ItemBridge]s inside an [ItemNetwork], the transfer rate of the
     * whole network is equal to the smallest one.
     */
    val itemTransferRate: Int
    
    /**
     * Gets the [ItemFilter] for the specified [ItemConnectionType].
     * Only [ItemConnectionType.INSERT] and [ItemConnectionType.EXTRACT] are valid here.
     */
    fun getFilter(type: ItemConnectionType, blockFace: BlockFace): ItemFilter?
    
}