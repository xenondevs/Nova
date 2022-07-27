package xyz.xenondevs.nova.integration.customitems.plugin

import dev.lone.itemsadder.api.CustomBlock
import dev.lone.itemsadder.api.CustomCrop
import dev.lone.itemsadder.api.CustomStack
import dev.lone.itemsadder.api.Events.ItemsAdderLoadDataEvent
import dev.lone.itemsadder.api.Events.ItemsAdderLoadDataEvent.Cause
import dev.lone.itemsadder.api.ItemsAdder
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.data.recipe.ModelDataTest
import xyz.xenondevs.nova.data.recipe.SingleItemTest
import xyz.xenondevs.nova.integration.customitems.CustomBlockType
import xyz.xenondevs.nova.integration.customitems.CustomItemService
import xyz.xenondevs.nova.integration.customitems.CustomItemServiceManager
import xyz.xenondevs.nova.integration.customitems.CustomItemType
import xyz.xenondevs.nova.util.item.customModelData
import xyz.xenondevs.nova.util.item.playPlaceSoundEffect
import xyz.xenondevs.nova.util.playBreakEffects

internal object ItemsAdder : CustomItemService {
    
    override val isInstalled = Bukkit.getPluginManager().getPlugin("ItemsAdder") != null
    override val requiresLoadDelay = true
    
    init {
        if (isInstalled) {
            Bukkit.getPluginManager().registerEvents(
                ItemsAdderLoadListener {
                    CustomItemServiceManager.READY_LATCH.countDown()
                },
                NOVA
            )
        }
    }
    
    override fun removeBlock(block: Block, playSound: Boolean, showParticles: Boolean): Boolean {
        val customBlock = CustomBlock.byAlreadyPlaced(block) ?: return false
        if (playSound || showParticles) customBlock.playBreakEffect()
        customBlock.remove()
        return true
    }
    
    override fun breakBlock(block: Block, tool: ItemStack?, playSound: Boolean, showParticles: Boolean): List<ItemStack>? {
        val customBlock = CustomBlock.byAlreadyPlaced(block)
        if (customBlock != null) {
            val loot = customBlock.getLoot(tool, true)
            if (playSound || showParticles) customBlock.playBreakEffect()
            customBlock.remove()
            return loot
        }
        
        val customCrop = CustomCrop.byAlreadyPlaced(block)
        if (customCrop != null) {
            val loot = customCrop.getLoot(tool)
            if (playSound || showParticles) block.playBreakEffects()
            block.type = Material.AIR
            return loot
        }
        
        return null
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
    
    override fun getItemByName(name: String): ItemStack? {
        val customItem = CustomStack.getInstance(name)
        return customItem?.itemStack
    }
    
    override fun getItemTest(name: String): SingleItemTest? {
        return getItemByName(name)?.let { ModelDataTest(it.type, intArrayOf(it.customModelData), it) }
    }
    
    override fun hasRecipe(key: NamespacedKey): Boolean {
        return ItemsAdder.isCustomRecipe(key)
    }
    
    override fun getId(item: ItemStack): String? {
        return CustomStack.byItemStack(item)?.namespacedID
    }
    
}

class ItemsAdderLoadListener(private val run: () -> Unit) : Listener {
    
    @EventHandler
    fun handleItemsAdderLoadData(event: ItemsAdderLoadDataEvent) {
        if (event.cause == Cause.FIRST_LOAD) {
            run()
        } else {
            LOGGER.warning("Reloading ItemsAdder might cause issues when items from ItemsAdder are used in Nova recipes.")
        }
    }
    
}