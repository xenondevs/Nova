package xyz.xenondevs.nova.integration.customitems.plugin

import io.th0rgal.oraxen.items.OraxenItems
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.recipe.ModelDataTest
import xyz.xenondevs.nova.data.recipe.SingleItemTest
import xyz.xenondevs.nova.integration.customitems.CustomBlockType
import xyz.xenondevs.nova.integration.customitems.CustomItemService
import xyz.xenondevs.nova.integration.customitems.CustomItemType
import xyz.xenondevs.nova.util.item.customModelData

internal object Oraxen : CustomItemService {
    
    override val isInstalled = Bukkit.getPluginManager().getPlugin("Oraxen") != null
    override val requiresLoadDelay = false
    
    override fun removeBlock(block: Block, playSound: Boolean, showParticles: Boolean): Boolean {
        // Missing API feature
        return false
    }
    
    override fun breakBlock(block: Block, tool: ItemStack?, playSound: Boolean, showParticles: Boolean): List<ItemStack>? {
        // Missing API feature
        return null
    }
    
    override fun getDrops(block: Block, tool: ItemStack?): List<ItemStack>? {
        // Missing API feature
        return null
    }
    
    override fun placeBlock(item: ItemStack, location: Location, playSound: Boolean): Boolean {
        // API for that is broken
        return true
    }
    
    override fun getItemType(item: ItemStack): CustomItemType? {
        return if (MMOItems.getId(item) != null) CustomItemType.NORMAL else null
    }
    
    override fun getBlockType(block: Block): CustomBlockType? {
        // Missing API feature
        return null
    }
    
    override fun getItemByName(name: String): ItemStack? {
        return OraxenItems.getItemById(name.removePrefix("oraxen:")).build()
    }
    
    override fun getItemTest(name: String): SingleItemTest? {
        return ItemsAdder.getItemByName(name)?.let { ModelDataTest(it.type, intArrayOf(it.customModelData), it) }
    }
    
    override fun getId(item: ItemStack): String? {
        val name = OraxenItems.getIdByItem(item) ?: return null
        return "oraxen:$name"
    }
    
    override fun hasRecipe(key: NamespacedKey): Boolean {
        return key.namespace == "oraxen"
    }
    
}