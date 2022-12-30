package xyz.xenondevs.nova.ui.config.side

import xyz.xenondevs.nova.tileentity.network.fluid.container.FluidContainer
import xyz.xenondevs.nova.tileentity.network.fluid.holder.FluidHolder

internal class FluidSideConfigGUI(
    holder: FluidHolder,
    containers: List<Pair<FluidContainer, String>>
) : ContainerSideConfigGUI<FluidContainer, FluidHolder>(holder, containers) {
    
    override val hasSimpleVersion = containers.size == 1
    override val hasAdvancedVersion = containers.size > 1
    
    override fun isSimpleConfiguration() = !hasAdvancedVersion
    
    init {
        require(containers.isNotEmpty())
        initGUI()
    }
    
}