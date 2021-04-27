package xyz.xenondevs.nova.network.item

import org.bukkit.inventory.ItemStack

class ItemFilter(
    var whitelist: Boolean,
    val items: Array<ItemStack?>
) {
    
    fun allowsItem(itemStack: ItemStack) =
        if (whitelist) items.any { it?.isSimilar(itemStack) ?: false }
        else items.none { it?.isSimilar(itemStack) ?: false }
    
}