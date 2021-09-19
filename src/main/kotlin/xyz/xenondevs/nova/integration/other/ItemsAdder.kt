package xyz.xenondevs.nova.integration.other

import dev.lone.itemsadder.api.CustomBlock
import dev.lone.itemsadder.api.CustomStack
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.integration.Integration
import xyz.xenondevs.nova.util.playPlaceSoundEffect

object ItemsAdder : Integration {
    override val isInstalled = Bukkit.getPluginManager().getPlugin("ItemsAdder") != null
    
    fun breakCustomBlock(block: CustomBlock): List<ItemStack> {
        val loot = block.getLoot(true)
        block.remove()
        return loot
    }
    
    fun placeItem(item: ItemStack, location: Location): Boolean {
        // Note: CustomBlock.byItemStack(item) can't be used because of an illegal cast in the ItemsAdder API
        val customItem = CustomStack.byItemStack(item)
        if (customItem == null || !customItem.isBlock)
            return false
        CustomBlock.place(customItem.namespacedID, location)
        Material.STONE.playPlaceSoundEffect(location)
        return true
    }
    
}