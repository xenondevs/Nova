package xyz.xenondevs.nova.ui.menu.sideconfig

import xyz.xenondevs.nova.tileentity.network.node.NetworkEndPoint
import xyz.xenondevs.nova.tileentity.network.type.DefaultNetworkTypes
import xyz.xenondevs.nova.tileentity.network.type.fluid.container.NetworkedFluidContainer
import xyz.xenondevs.nova.tileentity.network.type.fluid.holder.FluidHolder

internal class FluidSideConfigMenu(
    endPoint: NetworkEndPoint,
    holder: FluidHolder,
    containers: Map<NetworkedFluidContainer, String>
) : ContainerSideConfigMenu<NetworkedFluidContainer, FluidHolder>(endPoint, DefaultNetworkTypes.FLUID, holder, containers) {
    
    override val hasSimpleVersion = containers.size == 1
    override val hasAdvancedVersion = containers.size > 1
    
    override fun isSimpleConfiguration() = !hasAdvancedVersion
    
    init {
        require(containers.isNotEmpty())
    }
    
}