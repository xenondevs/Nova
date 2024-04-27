package xyz.xenondevs.nova.tileentity.network.type.item

import xyz.xenondevs.nova.tileentity.network.node.NetworkBridge

interface ItemBridge : NetworkBridge {
    
    /**
     * How many items can be transferred per second.
     */
    val itemTransferRate: Int
    
}