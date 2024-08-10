package xyz.xenondevs.nova.world.block.tileentity.network.type.energy

import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkBridge

interface EnergyBridge : NetworkBridge {
    
    /**
     * How much energy can be transferred per tick.
     */
    val energyTransferRate: Long
    
}