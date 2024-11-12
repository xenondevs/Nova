package xyz.xenondevs.nova.world.block.tileentity.network.type.fluid.container

import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.cbf.entry
import xyz.xenondevs.commons.provider.MutableProvider
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.defaultsTo
import xyz.xenondevs.nova.world.block.tileentity.network.type.fluid.FluidType
import java.util.*
import kotlin.math.min

/**
 * The default implementation for [NetworkedFluidContainer].
 */
class FluidContainer(
    override val uuid: UUID,
    override val allowedTypes: Set<FluidType>,
    val capacityProvider: Provider<Long>,
    val typeProvider: MutableProvider<FluidType?>,
    val amountProvider: MutableProvider<Long>
) : NetworkedFluidContainer {
    
    private val updateHandlers = ArrayList<() -> Unit>()
    override var type by typeProvider
    override var amount by amountProvider
    override val capacity by capacityProvider
    
    init {
        capacityProvider.subscribe {
            if (amount > it)
                amount = it
        }
    }
    
    constructor(
        compound: Provider<Compound>,
        uuid: UUID,
        allowedTypes: Set<FluidType>,
        capacityProvider: Provider<Long>
    ) : this(
        uuid,
        allowedTypes,
        capacityProvider,
        compound.entry("type"),
        compound.entry<Long>("amount").defaultsTo(0L)
    )
    
    override fun addFluid(type: FluidType, amount: Long): Long {
        if (amount == 0L)
            return 0L
        
        require(accepts(type)) { "Illegal fluid type: $type" }
        
        val toAdd = min(capacity - this.amount, amount)
        if (toAdd == 0L)
            return 0L
        
        this.type = type
        this.amount += toAdd
        
        callUpdateHandlers()
        
        return toAdd
    }
    
    override fun takeFluid(amount: Long): Long {
        val toTake = min(this.amount, amount)
        this.amount -= toTake
        if (this.amount == 0L)
            type = null
        
        callUpdateHandlers()
        
        return toTake
    }
    
    /**
     * Removes all fluid from this container.
     */
    fun clear() {
        amount = 0
        type = null
        
        callUpdateHandlers()
    }
    
    /**
     * Registers an update handler that is called when the container is updated.
     */
    fun addUpdateHandler(handler: () -> Unit) {
        updateHandlers.add(handler)
    }
    
    private fun callUpdateHandlers() {
        updateHandlers.forEach { it() }
    }
    
}