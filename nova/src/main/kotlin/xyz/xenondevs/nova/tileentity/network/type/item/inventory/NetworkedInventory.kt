package xyz.xenondevs.nova.tileentity.network.type.item.inventory

import net.minecraft.world.item.ItemStack
import xyz.xenondevs.nova.tileentity.network.node.EndPointContainer

interface NetworkedInventory : EndPointContainer {
    
    /**
     * The amount of inventory slots.
     */
    val size: Int
    
    /**
     * Gets the [ItemStack] on a specific slot. May or may not be a copy.
     */
    fun get(slot: Int): ItemStack
    
    /**
     * Places [itemStack] on [slot], may or may not be copied.
     */
    fun set(slot: Int, itemStack: ItemStack)
    
    /**
     * Adds an [amount] of [itemStack] to the inventory and returns how many items have been left over.
     * The [ItemStack.count] should be ignored, and the [itemStack] should not be modified.
     */
    fun add(itemStack: ItemStack, amount: Int): Int
    
    /**
     * Checks whether [amount] items can be taken from [slot].
     */
    fun canTake(slot: Int, amount: Int): Boolean
    
    /**
     * Takes [amount] items from [slot].
     */
    fun take(slot: Int, amount: Int)
    
    /**
     * Whether all slots of this inventory are filled up to their maximum stack size.
     */
    fun isFull(): Boolean
    
    /**
     * Whether all slots of this inventory are empty.
     */
    fun isEmpty(): Boolean
    
    /**
     * Copies the contents of this inventory to [destination].
     */
    fun copyContents(destination: Array<ItemStack>)
    
    /**
     * Whether this inventory is allowed to exchange items with [other].
     */
    fun canExchangeItemsWith(other: NetworkedInventory): Boolean {
        return this != other
    }
    
}