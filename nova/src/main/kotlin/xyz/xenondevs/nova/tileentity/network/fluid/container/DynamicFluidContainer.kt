package xyz.xenondevs.nova.tileentity.network.fluid.container

import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.mutable.MutableProvider
import xyz.xenondevs.nova.tileentity.network.fluid.FluidType
import java.util.*

/**
 * A [FluidContainer] with a dynamic capacity.
 */
class DynamicFluidContainer(
    uuid: UUID,
    allowedTypes: Set<FluidType>,
    typeProvider: MutableProvider<FluidType?>,
    capacityProvider: Provider<Long>,
    amountProvider: MutableProvider<Long>
) : FluidContainer(uuid, allowedTypes) {
    
    override var type by typeProvider
    override val capacity by capacityProvider
    override var amount by amountProvider
    
    init {
        capacityProvider.addUpdateHandler { 
            if (this.amount > capacity)
                this.amount = capacity
            
            callUpdateHandlers()
        }
    }
    
}