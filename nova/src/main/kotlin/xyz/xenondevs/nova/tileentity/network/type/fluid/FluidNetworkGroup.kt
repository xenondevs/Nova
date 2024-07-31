package xyz.xenondevs.nova.tileentity.network.type.fluid

import xyz.xenondevs.nova.tileentity.network.NetworkGroup
import xyz.xenondevs.nova.tileentity.network.NetworkGroupData

internal class FluidNetworkGroup(data: NetworkGroupData<FluidNetwork>) : NetworkGroup<FluidNetwork>, NetworkGroupData<FluidNetwork> by data {
    
    override fun tick() {
        networks.forEach(FluidNetwork::tick)
    }
    
    override fun postTickSync() {
        networks.forEach(FluidNetwork::postTickSync)
    }
    
}