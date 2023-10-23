package xyz.xenondevs.nova.integration.customitems

import net.kyori.adventure.text.Component
import net.minecraft.resources.ResourceLocation
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.recipe.SingleItemTest
import xyz.xenondevs.nova.data.resources.ResourcePath

object CustomItemServiceManager {
    
    internal val services = ArrayList<CustomItemService>()
    
    fun placeBlock(item: ItemStack, location: Location, playSound: Boolean): Boolean {
        return services.any { it.placeBlock(item, location, playSound) }
    }
    
    fun removeBlock(block: Block, breakEffects: Boolean): Boolean {
        return services.any { it.removeBlock(block, breakEffects) }
    }
    
    fun getDrops(block: Block, tool: ItemStack?): List<ItemStack>? {
        return services.firstNotNullOfOrNull { it.getDrops(block, tool) }
    }
    
    fun getItemType(item: ItemStack): CustomItemType? {
        return services.firstNotNullOfOrNull { it.getItemType(item) }
    }
    
    fun getBlockType(block: Block): CustomBlockType? {
        return services.firstNotNullOfOrNull { it.getBlockType(block) }
    }
    
    fun getItemByName(name: String): ItemStack? {
        return services.firstNotNullOfOrNull { it.getItemById(name) }
    }
    
    fun getItemTest(name: String): SingleItemTest? {
        return services.firstNotNullOfOrNull { it.getItemTest(name) }
    }
    
    fun getId(item: ItemStack): String? {
        return services.firstNotNullOfOrNull { it.getId(item) }
    }
    
    fun getId(block: Block): String? {
        return services.firstNotNullOfOrNull { it.getId(block) }
    }
    
    fun getName(item: ItemStack, locale: String): Component? {
        return services.firstNotNullOfOrNull { it.getName(item, locale) }
    }
    
    fun getName(block: Block, locale: String): Component? {
        return services.firstNotNullOfOrNull { it.getName(block, locale) }
    }
    
    fun hasRecipe(key: NamespacedKey): Boolean {
        return services.any { it.hasRecipe(key) }
    }
    
    fun canBreakBlock(block: Block, tool: ItemStack?): Boolean? {
        return services.firstNotNullOfOrNull { it.canBreakBlock(block, tool) }
    }
    
    fun getBlockItemModelPaths(): Map<ResourceLocation, ResourcePath> {
        val map = HashMap<ResourceLocation, ResourcePath>()
        services.forEach { map += it.getBlockItemModelPaths() }
        return map
    }
    
}