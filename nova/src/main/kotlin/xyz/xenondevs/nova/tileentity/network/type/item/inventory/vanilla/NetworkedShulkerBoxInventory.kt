package xyz.xenondevs.nova.tileentity.network.type.item.inventory.vanilla

import org.bukkit.Material
import org.bukkit.inventory.ItemStack

internal class NetworkedShulkerBoxInventory(container: ItemStackContainer) : NetworkedNMSInventory(container) {
    
    override fun add(itemStack: ItemStack, amount: Int): Int {
        if (isShulkerBox(itemStack.type))
            return itemStack.amount
        
        return super.add(itemStack, amount)
    }
    
    private fun isShulkerBox(material: Material): Boolean {
        return when (material) {
            Material.SHULKER_BOX, Material.WHITE_SHULKER_BOX, Material.ORANGE_SHULKER_BOX, Material.MAGENTA_SHULKER_BOX, Material.LIGHT_BLUE_SHULKER_BOX,
            Material.YELLOW_SHULKER_BOX, Material.LIME_SHULKER_BOX, Material.PINK_SHULKER_BOX, Material.GRAY_SHULKER_BOX, Material.LIGHT_GRAY_SHULKER_BOX,
            Material.CYAN_SHULKER_BOX, Material.PURPLE_SHULKER_BOX, Material.BLUE_SHULKER_BOX, Material.BROWN_SHULKER_BOX, Material.GREEN_SHULKER_BOX,
            Material.RED_SHULKER_BOX, Material.BLACK_SHULKER_BOX -> true
            
            else -> false
        }
    }
    
}