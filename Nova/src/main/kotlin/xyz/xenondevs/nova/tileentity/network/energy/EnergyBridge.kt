package xyz.xenondevs.nova.tileentity.network.energy

import xyz.xenondevs.nova.tileentity.network.NetworkBridge

interface EnergyBridge : NetworkBridge {
    
    /**
     * How much energy can be transferred per tick.
     */
    var energyTransferRate: Long
    
}