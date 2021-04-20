package xyz.xenondevs.nova.network.item

import xyz.xenondevs.nova.network.NetworkBridge

interface ItemBridge : NetworkBridge {
    
    /**
     * How many items can be transferred per tick. If there are different
     * types of [ItemBridge]s inside an [ItemNetwork], the transfer rate of the
     * whole network is equal to the smallest one.
     */
    val itemTransferRate: Int
    
}