package xyz.xenondevs.nova.network.energy

import xyz.xenondevs.nova.network.NetworkBridge

interface EnergyBridge : NetworkBridge {
    
    /**
     * How much energy can be transferred per tick. If there are different
     * types of [NetworkBridge]s inside an [EnergyNetwork], the transfer of the
     * whole network is equal to the smallest one.
     */
    val transferRate: Int
    
}