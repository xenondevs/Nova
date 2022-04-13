package xyz.xenondevs.nova.integration.customitems

import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.recipe.SingleItemTest
import xyz.xenondevs.nova.integration.InternalIntegration

interface CustomItemService : InternalIntegration {
    
    /**
     * If this [CustomItemService] requires the Nova initialization to be delayed
     */
    val requiresLoadDelay: Boolean
    
    /**
     * Remove a block from the world without handling drops
     * @return If the block was from this [CustomItemService] and has been removed successfully
     */
    fun removeBlock(block: Block, playEffects: Boolean): Boolean
    
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
     * Gets all drops of a block from this [CustomItemService]
     * @return The drops or null if the block isn't from this [CustomItemService]
     */
    fun getDrops(block: Block, tool: ItemStack?): List<ItemStack>?
    
    /**
     * Gets the [CustomItemType] of this [ItemStack] or null if the [ItemStack] is not
     * from this [CustomItemService]
     */
    fun getItemType(item: ItemStack): CustomItemType?
    
    /**
     * Gets the [CustomBlockType] of this [Block] or null if the [Block] is not
     * from this [CustomItemService]
     */
    fun getBlockType(block: Block): CustomBlockType?
    
    /**
     * Gets an [ItemStack] from a namespaced name
     */
    fun getItemByName(name: String): ItemStack?
    
    /**
     * Gets an [SingleItemTest] from a namespaced name
     */
    fun getItemTest(name: String): SingleItemTest?
    
    /**
     * Gets a namespaced name from an [ItemStack]
     */
    fun getId(item: ItemStack): String?
    
    /**
     * Checks if this [CustomItemService] registered a recipe with that [key]
     */
    fun hasRecipe(key: NamespacedKey): Boolean
    
}