package xyz.xenondevs.nova.ui.menu.sideconfig

import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkEndPoint
import xyz.xenondevs.nova.world.block.tileentity.network.type.DefaultNetworkTypes
import xyz.xenondevs.nova.world.block.tileentity.network.type.fluid.container.NetworkedFluidContainer
import xyz.xenondevs.nova.world.block.tileentity.network.type.fluid.holder.FluidHolder
import xyz.xenondevs.nova.world.format.NetworkState

class FluidSideConfigMenu(
    endPoint: NetworkEndPoint,
    holder: FluidHolder,
    containers: Map<NetworkedFluidContainer, String>
) : ContainerSideConfigMenu<NetworkedFluidContainer, FluidHolder>(endPoint, DefaultNetworkTypes.FLUID, holder, containers) {
    
    override fun isSimpleConfiguration() = namedContainers.size == 1
    
    init {
        require(containers.isNotEmpty())
    }
    
    override fun init(state: NetworkState) {
        super.init(state)
        simpleMode.set(if (namedContainers.size == 1) SimplicityMode.SIMPLE_ONLY else SimplicityMode.ADVANCED_ONLY)
    }
    
}