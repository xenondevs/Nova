package xyz.xenondevs.nova.world.block.tileentity.network.type.item

import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkBridge

interface ItemBridge : NetworkBridge {
    
    /**
     * How many items can be transferred per second.
     */
    val itemTransferRate: Int
    
}