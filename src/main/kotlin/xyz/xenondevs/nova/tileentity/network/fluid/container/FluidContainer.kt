package xyz.xenondevs.nova.tileentity.network.fluid.container

import xyz.xenondevs.nova.tileentity.network.fluid.FluidType
import java.util.*
import kotlin.math.min

abstract class FluidContainer(
    val uuid: UUID,
    allowedTypes: Set<FluidType>,
    type: FluidType?,
    amount: Long,
    capacity: Long
) {
    
    /**
     * A set of allowed fluid types the [type] can be set to.
     */
    val allowedTypes: Set<FluidType> = allowedTypes
    
    /**
     * The [FluidType] of this [FluidContainer] or null if
     * the container contains a non-fluid (powder snow in a cauldron).
     */
    open var type: FluidType? = type
        protected set
    
    /**
     * How many mB this container can hold.
     */
    var capacity: Long = capacity
        set(value) {
            field = value
            if (amount > value)
                amount = value
            
            callUpdateHandlers()
        }
    
    /**
     * The amount of mB in this container.
     */
    open var amount: Long = amount
        protected set
    
    val updateHandlers = ArrayList<() -> Unit>()
    
    /**
     * Adds fluid to this [FluidContainer].
     * @throws IllegalArgumentException If the specified [type] is invalid
     * @throws IllegalArgumentException If the [amount] would exceed this [container's][FluidContainer] capacity.
     */
    open fun addFluid(type: FluidType, amount: Long) {
        if (amount == 0L) return
        require(this.type == type || (this.type == FluidType.NONE && type in allowedTypes)) { "Illegal fluid type: $type" }
        require(this.capacity >= this.amount + amount) { "The provided amount $amount would exceed this container's capacity $capacity (current amount: ${this.amount})" }
        
        if (this.type == FluidType.NONE) this.type = type
        this.amount += amount
        
        callUpdateHandlers()
    }
    
    /**
     * Tries to add the given amount of fluid to this [FluidContainer].
     * @throws IllegalArgumentException If the specified [type] is invalid
     * @return The amount of fluid that was actually added.
     */
    open fun tryAddFluid(type: FluidType, amount: Long): Long {
        if (amount == 0L) return 0L
        require(this.type == type || (this.type == FluidType.NONE && type in allowedTypes)) { "Illegal fluid type: $type" }
        
        val toAdd = min(capacity - this.amount, amount)
        this.amount += toAdd
        if (this.type == FluidType.NONE) this.type = type
        
        callUpdateHandlers()
        
        return toAdd
    }
    
    /**
     * Takes fluid from this [FluidContainer].
     * @throws IllegalArgumentException If there are not at least [amount] mB in this container.
     */
    open fun takeFluid(amount: Long) {
        require(this.amount >= amount) { "FluidContainer does not contain ${amount}mB" }
        
        this.amount -= amount
        if (this.amount == 0L) type = FluidType.NONE
        
        callUpdateHandlers()
    }
    
    /**
     * Tries to take fluid from this [FluidContainer].
     * @return The amount of fluid that was actually taken.
     */
    open fun tryTakeFluid(amount: Long): Long {
        val toTake = min(this.amount, amount)
        this.amount -= toTake
        if (this.amount == 0L) type = FluidType.NONE
        callUpdateHandlers()
        
        return toTake
    }
    
    /**
     * Removes all fluids from this [FluidContainer].
     */
    open fun clear() {
        amount = 0
        type = FluidType.NONE
        
        callUpdateHandlers()
    }
    
    /**
     * Checks if this [FluidContainer] would accept [amount] of [type].
     */
    open fun accepts(type: FluidType, amount: Long): Boolean {
        return (this.type == type || (this.type == FluidType.NONE && type in allowedTypes)) && (this.amount + amount <= capacity)
    }
    
    open fun accepts(type: FluidType) : Boolean {
        return (this.type == type || (this.type == FluidType.NONE && type in allowedTypes))
    }
    
    /**
     * Checks if the [FluidContainer] is full.
     */
    open fun isFull(): Boolean {
        return this.amount >= this.capacity
    }
    
    /**
     * Checks if the [FluidContainer] has any fluid.
     */
    open fun hasFluid(): Boolean {
        return type != null && type != FluidType.NONE
    }
    
    /**
     * Checks if the [FluidContainer] is empty.
     */
    open fun isEmpty(): Boolean {
        return type == null || type == FluidType.NONE
    }
    
    protected fun callUpdateHandlers() =
        updateHandlers.forEach { it() }
    
}