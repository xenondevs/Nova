package xyz.xenondevs.nova.util

import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.CraftingInventory
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryView
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory
import xyz.xenondevs.invui.inventory.VirtualInventory
import xyz.xenondevs.invui.inventory.event.UpdateReason
import xyz.xenondevs.nova.util.item.takeUnlessEmpty

/**
 * Adds a [List] of [ItemStack]s to a [VirtualInventory].
 */
fun VirtualInventory.addAll(reason: UpdateReason?, items: List<ItemStack>) =
    items.forEach { addItem(reason, it) }

/**
 * Checks if an [Inventory] is full.
 */
fun Inventory.isFull(): Boolean {
    for (item in contents)
        if (item == null || item.amount < item.maxStackSize)
            return false
    return true
}

/**
 * Puts an [ItemStack] on the [prioritizedSlot] or adds it to the [Inventory][PlayerInventory]
 * if the given slot is occupied.
 */
fun PlayerInventory.addPrioritized(prioritizedSlot: EquipmentSlot, itemStack: ItemStack): Int {
    if (getItem(prioritizedSlot).takeUnlessEmpty() == null) {
        setItem(prioritizedSlot, itemStack)
        return 0
    }
    
    return addItemCorrectly(itemStack)
}

/**
 * Puts an [ItemStack] on the [prioritizedSlot] or adds it to the [Inventory]
 * if the given slot is occupied.
 */
fun Inventory.addPrioritized(prioritizedSlot: Int, itemStack: ItemStack): Int {
    if (getItem(prioritizedSlot) == null) {
        setItem(prioritizedSlot, itemStack)
        return 0
    }
    
    return addItemCorrectly(itemStack)
}

/**
 * If the [Player] has is currently looking into an inventory.
 * Does not detect the player's inventory itself because that is not sent to the server.
 */
val Player.hasInventoryOpen: Boolean
    get() = openInventory.topInventory.type != InventoryType.CRAFTING

/**
 * Checks if an [InventoryView] is the player inventory
 */
fun InventoryView.isPlayerView() = topInventory is CraftingInventory && topInventory.size == 5