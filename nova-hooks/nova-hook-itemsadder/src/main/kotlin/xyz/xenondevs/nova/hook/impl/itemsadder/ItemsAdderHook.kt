package xyz.xenondevs.nova.hook.impl.itemsadder

import dev.lone.itemsadder.api.CustomBlock
import dev.lone.itemsadder.api.CustomCrop
import dev.lone.itemsadder.api.CustomStack
import dev.lone.itemsadder.api.ItemsAdder
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.minecraft.resources.ResourceLocation
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.recipe.ModelDataTest
import xyz.xenondevs.nova.data.recipe.SingleItemTest
import xyz.xenondevs.nova.data.resources.ResourcePath
import xyz.xenondevs.nova.integration.Hook
import xyz.xenondevs.nova.integration.customitems.CustomBlockType
import xyz.xenondevs.nova.integration.customitems.CustomItemService
import xyz.xenondevs.nova.integration.customitems.CustomItemType
import xyz.xenondevs.nova.util.item.customModelData
import xyz.xenondevs.nova.util.item.playPlaceSoundEffect
import xyz.xenondevs.nova.util.playBreakEffects

@Hook(plugins = ["ItemsAdder"], loadListener = ItemsAdderLoadListener::class)
internal object ItemsAdderHook : CustomItemService {
    
    override fun removeBlock(block: Block, breakEffects: Boolean): Boolean {
        val customBlock = CustomBlock.byAlreadyPlaced(block)
        if (customBlock != null) {
            if (breakEffects) customBlock.playBreakEffect()
            customBlock.remove()
            return true
        }
        
        val customCrop = CustomCrop.byAlreadyPlaced(block)
        if (customCrop != null) {
            if (breakEffects) block.playBreakEffects()
            block.type = Material.AIR
            return true
        }
        
        return false
    }
    
    override fun getDrops(block: Block, tool: ItemStack?): List<ItemStack>? {
        val customBlock = CustomBlock.byAlreadyPlaced(block)
        if (customBlock != null)
            return customBlock.getLoot(tool, true)
        
        // Note: ItemsAdder throws an exception if the block is not a custom crop
        val customCrop = runCatching { CustomCrop.byAlreadyPlaced(block) }.getOrNull()
        if (customCrop != null)
            return customCrop.getLoot(tool)
        
        return null
    }
    
    override fun placeBlock(item: ItemStack, location: Location, playSound: Boolean): Boolean {
        // Note: CustomBlock.byItemStack(item) can't be used because of an illegal cast in the ItemsAdder API
        val customItem = CustomStack.byItemStack(item) ?: return false
        
        if (customItem.isBlock) {
            CustomBlock.place(customItem.namespacedID, location)?.playPlaceSound()
        } else if (CustomCrop.isSeed(item)) {
            CustomCrop.place(customItem.namespacedID, location)
            location.block.type.playPlaceSoundEffect(location)
        } else return false
        
        return true
    }
    
    override fun getItemType(item: ItemStack): CustomItemType? {
        CustomStack.byItemStack(item) ?: return null
        
        return if (CustomCrop.isSeed(item)) CustomItemType.SEED
        else CustomItemType.NORMAL
    }
    
    override fun getBlockType(block: Block): CustomBlockType? {
        return when {
            CustomBlock.byAlreadyPlaced(block) != null -> CustomBlockType.NORMAL
            runCatching { CustomCrop.byAlreadyPlaced(block) }.getOrNull() != null -> CustomBlockType.CROP
            else -> null
        }
    }
    
    override fun getItemById(id: String): ItemStack? {
        val customItem = CustomStack.getInstance(id)
        return customItem?.itemStack
    }
    
    override fun getItemTest(id: String): SingleItemTest? {
        return getItemById(id)?.let { ModelDataTest(it.type, intArrayOf(it.customModelData), it) }
    }
    
    override fun hasRecipe(key: NamespacedKey): Boolean {
        return ItemsAdder.isCustomRecipe(key)
    }
    
    override fun getId(item: ItemStack): String? {
        return CustomStack.byItemStack(item)?.namespacedID
    }
    
    override fun getId(block: Block): String? {
        return CustomBlock.byAlreadyPlaced(block)?.namespacedID
            ?: CustomCrop.byAlreadyPlaced(block)?.seed?.namespacedID
    }
    
    override fun getName(item: ItemStack, locale: String): Component? {
        val customStack = CustomStack.byItemStack(item) ?: return null
        return customStack.displayName?.let(LegacyComponentSerializer.legacySection()::deserialize) ?: Component.empty()
    }
    
    override fun getName(block: Block, locale: String): Component? {
        val customBlock = CustomBlock.byAlreadyPlaced(block)
        if (customBlock != null) {
            return customBlock.displayName
                ?.let(LegacyComponentSerializer.legacySection()::deserialize)
                ?: Component.empty()
        }
        
        val customCrop = CustomCrop.byAlreadyPlaced(block)?.seed
        if (customCrop != null) {
            return customCrop.displayName
                ?.let(LegacyComponentSerializer.legacySection()::deserialize)
                ?: Component.empty()
        }
        
        return null
    }
    
    override fun canBreakBlock(block: Block, tool: ItemStack?): Boolean? {
        // Missing API method
        return null
    }
    
    override fun getBlockItemModelPaths(): Map<ResourceLocation, ResourcePath> {
        return ItemsAdder.getAllItems()
            .filter { it.isBlock || CustomCrop.isSeed(it.itemStack) }
            .map(CustomStack::getNamespacedID)
            .associateTo(HashMap()) {
                val path = ItemsAdder.Advanced.getItemModelResourceLocation(it)!!.substringBeforeLast('.')
                ResourceLocation(it) to ResourcePath.of(path)
            }
    }
    
}