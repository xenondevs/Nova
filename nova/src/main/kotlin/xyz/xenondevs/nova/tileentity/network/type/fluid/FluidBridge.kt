package xyz.xenondevs.nova.tileentity.network.type.fluid

import xyz.xenondevs.nova.tileentity.network.node.NetworkBridge

interface FluidBridge : NetworkBridge {
    
    /**
     * How much fluid can be transferred per tick.
     */
    val fluidTransferRate: Long
    
}