package xyz.xenondevs.nova.integration.customitems

import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.integration.Integration

interface CustomItemService : Integration {
    
    /**
     * If this [CustomItemService] requires the Nova initialization to be delayed
     */
    val requiresLoadDelay: Boolean
    
    /**
     * Breaks a block from this [CustomItemService]
     * @return the drops or null if the block isn't from this [CustomItemService]
     */
    fun breakBlock(block: Block, tool: ItemStack?, playEffects: Boolean): List<ItemStack>?
    
    /**
     * Places an item from this [CustomItemService]
     *
     * @return If the item is a block from this [CustomItemService] and has been placed
     * successfully
     */
    fun placeBlock(item: ItemStack, location: Location, playEffects: Boolean): Boolean
    
    /**
     * Gets an [ItemStack] from a namespaced name
     */
    fun getItemByName(name: String): ItemStack?
    
    /**
     * Gets a namespaced name from an [ItemStack]
     */
    fun getNameKey(item: ItemStack): String?
    
    /**
     * Checks if this [CustomItemService] uses this namespace
     */
    fun hasNamespace(namespace: String): Boolean
    
}