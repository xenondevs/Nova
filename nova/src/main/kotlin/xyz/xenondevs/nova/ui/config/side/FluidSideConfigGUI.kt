package xyz.xenondevs.nova.ui.config.side

import xyz.xenondevs.nova.tileentity.network.fluid.container.FluidContainer
import xyz.xenondevs.nova.tileentity.network.fluid.holder.FluidHolder

internal class FluidSideConfigGUI(
    holder: FluidHolder,
    containers: List<Pair<FluidContainer, String>>
) : ContainerSideConfigGUI<FluidContainer, FluidHolder>(holder, containers) {
    
    override val hasSimpleVersion = false
    override fun isSimpleConfiguration() = false
    
    init {
        initGUI()
    }
    
}