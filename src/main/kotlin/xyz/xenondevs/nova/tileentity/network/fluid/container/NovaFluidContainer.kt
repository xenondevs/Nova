package xyz.xenondevs.nova.tileentity.network.fluid.container

import xyz.xenondevs.nova.data.serialization.cbf.element.CompoundElement
import xyz.xenondevs.nova.tileentity.network.fluid.FluidType
import java.util.*

class NovaFluidContainer(
    uuid: UUID,
    allowedTypes: Set<FluidType>,
    type: FluidType?,
    capacity: Long,
    amount: Long,
) : FluidContainer(uuid, allowedTypes, type, capacity, amount) {
    
    constructor(uuid: UUID, allowedTypes: Set<FluidType>, compound: CompoundElement) : this(
        uuid,
        allowedTypes,
        compound.getEnumConstant<FluidType>("type")!!,
        compound.getAsserted("capacity"),
        compound.getAsserted("amount")
    )
    
    /**
     * Serializes the [type], [capacity] and [amount] into a [CompoundElement].
     */
    fun serialize(): CompoundElement {
        val compound = CompoundElement()
        compound.put("type", type)
        compound.put("capacity", capacity)
        compound.put("amount", amount)
        
        return compound
    }
    
}