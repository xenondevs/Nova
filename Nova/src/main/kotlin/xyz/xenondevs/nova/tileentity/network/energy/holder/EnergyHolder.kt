package xyz.xenondevs.nova.tileentity.network.energy.holder

import xyz.xenondevs.nova.tileentity.network.EndPointDataHolder
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType

interface EnergyHolder : EndPointDataHolder {
    
    val allowedConnectionType: NetworkConnectionType
    var energy: Long
    val requestedEnergy: Long
    
}