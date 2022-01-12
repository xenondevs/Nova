package xyz.xenondevs.nova.integration.customitems.plugin

import io.lumine.mythic.lib.api.item.NBTItem
import net.Indyuce.mmoitems.api.Type
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.recipe.ItemTest
import xyz.xenondevs.nova.integration.customitems.CustomItemService
import net.Indyuce.mmoitems.MMOItems as MMOItemsPlugin

object MMOItems : CustomItemService {
    
    override val isInstalled = Bukkit.getPluginManager().getPlugin("MMOItems") != null
    override val requiresLoadDelay = false
    
    private lateinit var mmoItems: MMOItemsPlugin
    private lateinit var itemTypes: Collection<Type>
    
    init {
        if (isInstalled) {
            mmoItems = MMOItemsPlugin.plugin
            itemTypes = mmoItems.types.all
        }
    }
    
    override fun removeBlock(block: Block, playEffects: Boolean): Boolean {
        if (mmoItems.customBlocks.getFromBlock(block.blockData).isEmpty) return false
        block.type = Material.AIR
        return true
    }
    
    override fun breakBlock(block: Block, tool: ItemStack?, playEffects: Boolean): List<ItemStack>? {
        val customBlock = mmoItems.customBlocks.getFromBlock(block.blockData).orElse(null) ?: return null
        block.type = Material.AIR
        return listOf(customBlock.item)
    }
    
    override fun placeBlock(item: ItemStack, location: Location, playEffects: Boolean): Boolean {
        val block = location.block
        if (!mmoItems.customBlocks.isMushroomBlock(block.type)) {
            val nbtItem = NBTItem.get(item)
            val blockId = nbtItem.getInteger("MMOITEMS_BLOCK_ID")
                .takeUnless { it > 160 || it < 1 || it == 54 }
                ?: return false
            
            val customBlock = mmoItems.customBlocks.getBlock(blockId) ?: return false
            block.setType(customBlock.state.type, false)
            block.setBlockData(customBlock.state.blockData, false)
            
            return true
        }
        
        return false
    }
    
    override fun getDrops(block: Block, tool: ItemStack?): List<ItemStack>? {
        return mmoItems.customBlocks.getFromBlock(block.blockData).orElse(null)?.item?.let(::listOf)
    }
    
    override fun getItemByName(name: String): ItemStack? {
        if (name.startsWith("mmoitems:")) {
            val itemName = name.removePrefix("mmoitems:").uppercase()
            return itemTypes.firstNotNullOfOrNull { mmoItems.getItem(it, itemName) }
        }
        
        return null
    }
    
    override fun getItemTest(name: String): ItemTest? {
        return getItemByName(name)?.let { MMOItemTest(name, it) }
    }
    
    override fun getId(item: ItemStack): String? {
        val id = NBTItem.get(item).getString("MMOITEMS_ITEM_ID")?.takeUnless(String::isBlank) ?: return null
        return "mmoitems:$id"
    }
    
    override fun hasRecipe(key: NamespacedKey): Boolean {
        return key.namespace == "mmoitems"
    }
    
}

private class MMOItemTest(private val id: String, override val example: ItemStack) : ItemTest {
    
    override fun test(item: ItemStack): Boolean {
        return id.equals(MMOItems.getId(item), true)
    }
    
}