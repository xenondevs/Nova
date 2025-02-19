package xyz.xenondevs.nova.hook.impl.mmoitems

import io.lumine.mythic.lib.api.item.NBTItem
import net.Indyuce.mmoitems.MMOItems
import net.Indyuce.mmoitems.api.Type
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.integration.Hook
import xyz.xenondevs.nova.integration.customitems.CustomBlockType
import xyz.xenondevs.nova.integration.customitems.CustomItemService
import xyz.xenondevs.nova.integration.customitems.CustomItemType
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.ResourceType
import xyz.xenondevs.nova.world.item.recipe.SingleItemTest
import net.Indyuce.mmoitems.MMOItems as MMOItemsPlugin

@Hook(plugins = ["MMOItems"])
internal object MMOItemsHook : CustomItemService {
    
    private val MMO_ITEMS: MMOItems = MMOItemsPlugin.plugin
    private val ITEM_TYPES: Collection<Type> = MMO_ITEMS.types.all
    
    override fun removeBlock(block: Block, breakEffects: Boolean): Boolean {
        if (MMO_ITEMS.customBlocks.getFromBlock(block.blockData).isEmpty) return false
        block.type = Material.AIR
        return true
    }
    
    override fun placeBlock(item: ItemStack, location: Location, playSound: Boolean): Boolean {
        val block = location.block
        if (!MMO_ITEMS.customBlocks.isMushroomBlock(block.type)) {
            val nbtItem = NBTItem.get(item)
            val blockId = nbtItem.getInteger("MMOITEMS_BLOCK_ID")
                .takeUnless { it > 160 || it < 1 || it == 54 }
                ?: return false
            
            val customBlock = MMO_ITEMS.customBlocks.getBlock(blockId) ?: return false
            block.setType(customBlock.state.type, false)
            block.setBlockData(customBlock.state.blockData, false)
            
            return true
        }
        
        return false
    }
    
    override fun getDrops(block: Block, tool: ItemStack?): List<ItemStack>? {
        return MMO_ITEMS.customBlocks.getFromBlock(block.blockData).orElse(null)?.item?.let(::listOf)
    }
    
    override fun getItemType(item: ItemStack): CustomItemType? {
        return if (getId(item) != null) CustomItemType.NORMAL else null
    }
    
    override fun getBlockType(block: Block): CustomBlockType? {
        return if (MMO_ITEMS.customBlocks.getFromBlock(block.blockData).isEmpty) null else CustomBlockType.NORMAL
    }
    
    override fun getItemById(id: String): ItemStack? {
        if (id.startsWith("mmoitems:")) {
            val itemName = id.removePrefix("mmoitems:").uppercase()
            return ITEM_TYPES.firstNotNullOfOrNull { MMO_ITEMS.getItem(it, itemName) }
        }
        
        return null
    }
    
    override fun getItemTest(id: String): SingleItemTest? {
        return getItemById(id)?.let { MMOItemTest(id, it) }
    }
    
    override fun getId(item: ItemStack): String? {
        val id = NBTItem.get(item).getString("MMOITEMS_ITEM_ID")?.takeUnless(String::isBlank) ?: return null
        return "mmoitems:$id"
    }
    
    override fun getId(block: Block): String? {
        return MMO_ITEMS.customBlocks.getFromBlock(block.blockData).orElse(null)?.item?.let(MMOItemsHook::getId)
    }
    
    override fun getName(item: ItemStack, locale: String): Component? {
        if (getId(item) == null)
            return null
        
        return item.displayName()
    }
    
    override fun getName(block: Block, locale: String): Component? {
        val item = MMO_ITEMS.customBlocks.getFromBlock(block.blockData).orElse(null)?.item ?: return null
        return item.displayName()
    }
    
    override fun canBreakBlock(block: Block, tool: ItemStack?): Boolean? {
        return null
    }
    
    override fun getBlockItemModelPaths(): Map<Key, ResourcePath<ResourceType.Model>> {
        return emptyMap()
    }
    
}

private class MMOItemTest(private val id: String, override val example: ItemStack) : SingleItemTest {
    
    override fun test(item: ItemStack): Boolean {
        return id.equals(MMOItemsHook.getId(item), true)
    }
    
}