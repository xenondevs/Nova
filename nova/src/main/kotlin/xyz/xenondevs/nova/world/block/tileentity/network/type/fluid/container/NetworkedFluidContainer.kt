package xyz.xenondevs.nova.world.block.tileentity.network.type.fluid.container

import xyz.xenondevs.nova.world.block.tileentity.network.node.EndPointContainer
import xyz.xenondevs.nova.world.block.tileentity.network.type.fluid.FluidType

/**
 * A container that stores fluids.
 */
interface NetworkedFluidContainer : EndPointContainer {
    
    /**
     * The allowed fluid types that can be stored in this [NetworkedFluidContainer].
     */
    val allowedTypes: Set<FluidType>
    
    /**
     * The amount of fluid that can fit in this [NetworkedFluidContainer].
     */
    val capacity: Long
    
    /**
     * The current [FluidType] of this [NetworkedFluidContainer] or null if the container does not contain any fluid.
     */
    val type: FluidType?
    
    /**
     * The amount of fluid currently in this [NetworkedFluidContainer].
     */
    val amount: Long
    
    /**
     * Tries to add [amount] of [type] to this [NetworkedFluidContainer], then returns the amount that was actually added.
     */
    fun addFluid(type: FluidType, amount: Long): Long
    
    /**
     * Tries to take [amount] of fluid from this [NetworkedFluidContainer], then returns the amount that was actually taken.
     */
    fun takeFluid(amount: Long): Long
    
    /**
     * Whether this [NetworkedFluidContainer] can accept any more fluid.
     */
    fun isFull(): Boolean = amount >= capacity
    
    /**
     * Whether this [NetworkedFluidContainer] is empty.
     */
    fun isEmpty(): Boolean = type == null
    
    /**
     * Whether this [NetworkedFluidContainer] can accept [amount] of [type].
     */
    fun accepts(type: FluidType, amount: Long): Boolean =
        accepts(type) && this.amount + amount in 0..capacity
    
    /**
     * Whether this [NetworkedFluidContainer] can accept [type].
     */
    fun accepts(type: FluidType): Boolean =
        this.type == type || (this.type == null && type in allowedTypes)
    
}