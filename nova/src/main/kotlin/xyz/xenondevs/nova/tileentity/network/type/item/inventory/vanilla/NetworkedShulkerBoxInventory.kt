package xyz.xenondevs.nova.tileentity.network.type.item.inventory.vanilla

import org.bukkit.Tag
import org.bukkit.inventory.ItemStack

internal class NetworkedShulkerBoxInventory(container: MojangStackContainer) : NetworkedNMSInventory(container) {
    
    override fun addItem(item: ItemStack): Int {
        if (Tag.SHULKER_BOXES.isTagged(item.type))
            return item.amount
        
        return super.addItem(item)
    }
    
}