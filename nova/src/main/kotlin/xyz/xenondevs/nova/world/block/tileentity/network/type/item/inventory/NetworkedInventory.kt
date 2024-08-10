package xyz.xenondevs.nova.world.block.tileentity.network.type.item.inventory

import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.world.block.tileentity.network.node.EndPointContainer

interface NetworkedInventory : EndPointContainer {
    
    /**
     * The amount of inventory slots.
     */
    val size: Int
    
    /**
     * Adds an [amount] of [itemStack] to the inventory and returns how many items have been left over.
     * The [ItemStack.getAmount] should be ignored, and the [itemStack] should not be modified.
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