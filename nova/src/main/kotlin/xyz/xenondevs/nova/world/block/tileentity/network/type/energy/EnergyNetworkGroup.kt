package xyz.xenondevs.nova.world.block.tileentity.network.type.energy

import xyz.xenondevs.nova.world.block.tileentity.network.NetworkGroup
import xyz.xenondevs.nova.world.block.tileentity.network.NetworkGroupData
import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkEndPoint
import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkNodeConnection
import xyz.xenondevs.nova.world.block.tileentity.network.type.energy.holder.DefaultEnergyHolder

internal class EnergyNetworkGroup(data: NetworkGroupData<EnergyNetwork>) : NetworkGroup<EnergyNetwork>, NetworkGroupData<EnergyNetwork> by data {
    
    private val defaultHolders: Set<DefaultEnergyHolder> = networks.asSequence()
        .flatMap { it.nodes.values.asSequence().map(NetworkNodeConnection::node)  }
        .filterIsInstance<NetworkEndPoint>()
        .flatMap { it.holders.asSequence() }
        .filterIsInstance<DefaultEnergyHolder>()
        .toHashSet()
    
    override fun tick() {
        networks.forEach(EnergyNetwork::tick)
    }
    
    override fun postTick() {
        for (energyHolder in defaultHolders) {
            energyHolder.postTick()
        }
    }
    
}