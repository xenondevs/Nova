package xyz.xenondevs.nova.tileentity.network.type.item.inventory

import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.tileentity.network.node.EndPointContainer

interface NetworkedInventory : EndPointContainer {
    
    /**
     * How many slots the inventory has.
     */
    val size: Int
    
    /**
     * A copy of all the [ItemStack]s in this inventory.
     */
    val items: Array<ItemStack?>
    
    /**
     * Adds an [ItemStack] to the inventory and returns
     * how many items have been left over.
     */
    fun addItem(item: ItemStack): Int
    
    /**
     * Changes the [ItemStack] on a specific slot to the
     * specified [ItemStack].
     * @return If the action was successful
     */
    fun setItem(slot: Int, item: ItemStack?): Boolean
    
    /**
     * Gets the [ItemStack] on a specific slot.
     */
    fun getItem(slot: Int) = items[slot]
    
    /**
     * If the amount of the [ItemStack] on that [slot] can be decremented by one.
     */
    fun canDecrementByOne(slot: Int): Boolean = true
    
    /**
     * Decrements the amount of an [ItemStack] on a [slot] by one.
     */
    fun decrementByOne(slot: Int)
    
    /**
     * If all slots of this inventory are filled up to their max stack size
     */
    fun isFull(): Boolean
    
    /**
     * If this inventory is allowed to exchange items with [other].
     */
    fun canExchangeItemsWith(other: NetworkedInventory): Boolean {
        return this != other
    }
    
}