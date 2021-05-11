package xyz.xenondevs.nova.util

import de.studiocode.invui.virtualinventory.VirtualInventory
import de.studiocode.invui.virtualinventory.event.UpdateReason
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

/**
 * Adds a [List] of [ItemStack]s to a [VirtualInventory].
 */
fun VirtualInventory.addAll(reason: UpdateReason?, items: List<ItemStack>) =
    items.forEach { addItem(reason, it) }

/**
 * Adds an [ItemStack] to an [Inventory] while respecting both
 * the max stack size of the inventory as well as the max stack size
 * of the item type.
 *
 * Unlike Bukkit's addItem method, the [ItemStack] provided as the
 * method parameter will not be modified.
 *
 * @return The amount of items that did not fit.
 */
fun Inventory.addItemCorrectly(itemStack: ItemStack): Int {
    val typeMaxStackSize = itemStack.type.maxStackSize
    var amountLeft = itemStack.amount
    
    // add to partial slots
    while (amountLeft > 0) {
        val partialStack = getFirstPartialStack(itemStack) ?: break
        val partialAmount = partialStack.amount
        val addableAmount = minOf(amountLeft, maxStackSize - partialAmount, typeMaxStackSize - partialAmount).coerceAtLeast(0)
        partialStack.amount += addableAmount
        amountLeft -= addableAmount
    }
    
    // add to full slots
    while (amountLeft > 0) {
        val emptySlot = getFirstEmptySlot() ?: break
        val addableAmount = minOf(amountLeft, maxStackSize, typeMaxStackSize)
        setItem(emptySlot, itemStack.clone().apply { amount = addableAmount })
        amountLeft -= addableAmount
    }
    
    return amountLeft
}

/**
 * Gets the first [ItemStack] in the [Inventory.getStorageContents]
 * that is similar to [type] and not a full stack.
 */
fun Inventory.getFirstPartialStack(type: ItemStack): ItemStack? {
    val maxStackSize = type.type.maxStackSize
    for (item in storageContents) {
        if (type.isSimilar(item)) {
            val amount = item.amount
            if (amount < item.type.maxStackSize && amount < maxStackSize)
                return item
        }
    }
    
    return null
}

/**
 * Gets the first slot index of the [Inventory.getStorageContents]
 * that is completely empty.
 */
fun Inventory.getFirstEmptySlot(): Int? {
    for ((slot, item) in storageContents.withIndex()) {
        if (item == null) return slot
    }
    
    return null
}
