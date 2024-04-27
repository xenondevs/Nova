package xyz.xenondevs.nova.tileentity.network.type.fluid.container

import xyz.xenondevs.nova.tileentity.network.type.fluid.FluidType
import java.util.*
import kotlin.math.min

/**
 * The default implementation for [NetworkedFluidContainer].
 */
abstract class FluidContainer(
    override val uuid: UUID,
    override val allowedTypes: Set<FluidType>,
) : NetworkedFluidContainer {
    
    private val updateHandlers = ArrayList<() -> Unit>()
    
    abstract override var type: FluidType?
    abstract override var amount: Long
    
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
    open fun clear() {
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
    
    protected fun callUpdateHandlers() {
        updateHandlers.forEach { it() }
    }
    
}