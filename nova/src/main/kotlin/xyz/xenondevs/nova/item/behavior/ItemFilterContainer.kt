package xyz.xenondevs.nova.item.behavior

import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.tileentity.network.type.item.ItemFilter

/**
 * Interface for item behaviors that contain an [ItemFilter] of type [T].
 */
interface ItemFilterContainer<T : ItemFilter<T>> {
    
    /**
     * Reads the [ItemFilter] from the given [itemStack].
     */
    fun getFilter(itemStack: ItemStack): T?
    
    /**
     * Stores the given [filter] in the [itemStack].
     */
    fun setFilter(itemStack: ItemStack, filter: T?)
    
}