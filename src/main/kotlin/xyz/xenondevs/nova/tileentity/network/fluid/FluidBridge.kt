package xyz.xenondevs.nova.tileentity.network.fluid

import xyz.xenondevs.nova.tileentity.network.NetworkBridge

interface FluidBridge : NetworkBridge {
    
    /**
     * How much fluid can be transferred per tick.
     */
    val fluidTransferRate: Long
    
}