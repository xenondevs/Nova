package xyz.xenondevs.nova.tileentity.network.type.fluid.container

import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.cbf.provider.entry
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.mutable.defaultsTo
import xyz.xenondevs.nova.tileentity.network.type.fluid.FluidType
import java.util.*

/**
 * A [FluidContainer] with a dynamic capacity.
 */
class DynamicFluidContainer(
    compound: Provider<Compound>,
    uuid: UUID,
    allowedTypes: Set<FluidType>,
    capacityProvider: Provider<Long>,
) : FluidContainer(uuid, allowedTypes) {
    
    override var type: FluidType? by compound.entry("type")
    override var amount: Long by compound.entry<Long>("amount").defaultsTo(0L)
    override val capacity by capacityProvider
    
    init {
        capacityProvider.subscribe {
        if (this.amount > capacity)
                this.amount = capacity
            
            callUpdateHandlers()
        }
    }
    
}