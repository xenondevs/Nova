package xyz.xenondevs.nova.tileentity.network.type.item.inventory.vanilla

import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

internal class NetworkedShulkerBoxInventory(container: ItemStackContainer) : NetworkedNMSInventory(container) {
    
    override fun add(itemStack: ItemStack, amount: Int): Int {
        if (isShulkerBox(itemStack.item))
            return itemStack.count
        
        return super.add(itemStack, amount)
    }
    
    private fun isShulkerBox(item: Item): Boolean {
        return when (item) {
            Items.SHULKER_BOX, Items.WHITE_SHULKER_BOX, Items.ORANGE_SHULKER_BOX, Items.MAGENTA_SHULKER_BOX, Items.LIGHT_BLUE_SHULKER_BOX,
            Items.YELLOW_SHULKER_BOX, Items.LIME_SHULKER_BOX, Items.PINK_SHULKER_BOX, Items.GRAY_SHULKER_BOX, Items.LIGHT_GRAY_SHULKER_BOX,
            Items.CYAN_SHULKER_BOX, Items.PURPLE_SHULKER_BOX, Items.BLUE_SHULKER_BOX, Items.BROWN_SHULKER_BOX, Items.GREEN_SHULKER_BOX,
            Items.RED_SHULKER_BOX, Items.BLACK_SHULKER_BOX -> true
            
            else -> false
        }
    }
    
}