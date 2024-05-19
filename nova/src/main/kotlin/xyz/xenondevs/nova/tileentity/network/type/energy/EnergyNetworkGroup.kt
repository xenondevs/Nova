package xyz.xenondevs.nova.tileentity.network.type.energy

import xyz.xenondevs.nova.tileentity.network.NetworkGroup
import xyz.xenondevs.nova.tileentity.network.NetworkGroupData

internal class EnergyNetworkGroup(data: NetworkGroupData<EnergyNetwork>) : NetworkGroup<EnergyNetwork>, NetworkGroupData<EnergyNetwork> by data {
    
    override fun tick() {
        networks.forEach(EnergyNetwork::tick)
    }
    
}