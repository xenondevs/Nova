package xyz.xenondevs.nova.tileentity.network.type.energy

import xyz.xenondevs.nova.tileentity.network.node.NetworkBridge

interface EnergyBridge : NetworkBridge {
    
    /**
     * How much energy can be transferred per tick.
     */
    val energyTransferRate: Long
    
}