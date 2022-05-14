package xyz.xenondevs.nova.tileentity.network.item

import xyz.xenondevs.nova.tileentity.network.NetworkBridge

interface ItemBridge : NetworkBridge {
    
    /**
     * How many items can be transferred per second.
     */
    var itemTransferRate: Int
    
}