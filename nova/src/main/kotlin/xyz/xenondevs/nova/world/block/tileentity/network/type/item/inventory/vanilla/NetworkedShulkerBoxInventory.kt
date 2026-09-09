package xyz.xenondevs.nova.world.block.tileentity.network.type.item.inventory.vanilla

import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.registry.tags.ItemTypeTags
import xyz.xenondevs.nova.world.item.itemType

internal class NetworkedShulkerBoxInventory(container: ItemStackContainer) : NetworkedNMSInventory(container) {
    
    override fun add(itemStack: ItemStack, amount: Int): Int {
        if (itemStack.itemType in ItemTypeTags.SHULKER_BOXES)
            return itemStack.amount
        
        return super.add(itemStack, amount)
    }
    
}