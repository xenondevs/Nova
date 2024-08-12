package xyz.xenondevs.nova.world.block.tileentity.network.type.fluid

import xyz.xenondevs.nova.world.block.tileentity.network.NetworkGroup
import xyz.xenondevs.nova.world.block.tileentity.network.NetworkGroupData

internal class FluidNetworkGroup(data: NetworkGroupData<FluidNetwork>) : NetworkGroup<FluidNetwork>, NetworkGroupData<FluidNetwork> by data {
    
    override fun tick() {
        networks.forEach(FluidNetwork::tick)
    }
    
    override fun postTickSync() {
        networks.forEach(FluidNetwork::postTickSync)
    }
    
}