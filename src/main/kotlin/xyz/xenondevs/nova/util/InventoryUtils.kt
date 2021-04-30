package xyz.xenondevs.nova.util

import de.studiocode.invui.virtualinventory.VirtualInventory
import de.studiocode.invui.virtualinventory.event.UpdateReason
import org.bukkit.inventory.ItemStack

fun VirtualInventory.canFit(items: List<ItemStack>) =
    when {
        items.isEmpty() -> true
        items.size == 1 -> simulateAdd(items[0]) == 0
        else -> simulateMultiAdd(items).all { it == 0 }
    }

fun VirtualInventory.addAll(reason: UpdateReason?, items: List<ItemStack>) =
    items.forEach { addItem(reason, it) }
