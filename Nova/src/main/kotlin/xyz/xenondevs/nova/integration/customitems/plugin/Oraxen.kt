package xyz.xenondevs.nova.integration.customitems.plugin

import io.th0rgal.oraxen.items.OraxenItems
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.integration.customitems.CustomItemService

object Oraxen : CustomItemService {
    
    override val isInstalled = Bukkit.getPluginManager().getPlugin("Oraxen") != null
    override val requiresLoadDelay = false
    
    override fun breakBlock(block: Block, tool: ItemStack?, playEffects: Boolean): List<ItemStack>? {
        // Missing API feature
        return null
    }
    
    override fun placeBlock(item: ItemStack, location: Location, playEffects: Boolean): Boolean {
//        val id = OraxenItems.getIdByItem(item) ?: return false
//        BlockMechanicFactory.setBlockModel(location.block, id)
        return true
    }
    
    override fun getItemByName(name: String): ItemStack? {
        return OraxenItems.getItemById(name.removePrefix("oraxen:")).build()
    }
    
    override fun getNameKey(item: ItemStack): String? {
        val name = OraxenItems.getIdByItem(item) ?: return null
        return "oraxen:$name"
    }
    
    override fun hasNamespace(namespace: String): Boolean {
        return namespace == "oraxen"
    }
    
}