package xyz.xenondevs.nova.util

import de.studiocode.invui.virtualinventory.VirtualInventory
import de.studiocode.invui.virtualinventory.event.UpdateReason
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory

/**
 * Adds a [List] of [ItemStack]s to a [VirtualInventory].
 */
fun VirtualInventory.addAll(reason: UpdateReason?, items: List<ItemStack>) =
    items.forEach { addItem(reason, it) }


/**
 * Checks if a [VirtualInventory] is full.
 */
fun VirtualInventory.isFull(): Boolean {
    for (item in items)
        if (item == null || item.amount < item.type.maxStackSize)
            return false
    return true
}

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

/**
 * Puts an [ItemStack] on the [prioritizedSlot] or adds it to the [Inventory][PlayerInventory]
 * if the given slot is occupied.
 */
fun PlayerInventory.addPrioritized(prioritizedSlot: EquipmentSlot, itemStack: ItemStack) {
    if (getItem(prioritizedSlot).type == Material.AIR) setItem(prioritizedSlot, itemStack)
    else addItem(itemStack)
}

/**
 * Puts an [ItemStack] on the [prioritizedSlot] or adds it to the [Inventory]
 * if the given slot is occupied.
 */
fun Inventory.addPrioritized(prioritizedSlot: Int, itemStack: ItemStack) {
    if (getItem(prioritizedSlot) == null) setItem(prioritizedSlot, itemStack)
    else addItem(itemStack)
}

/**
 * If the [Player] has is currently looking into an inventory.
 * Does not detect the player's inventory itself because that is not sent to the server.
 */
val Player.hasInventoryOpen: Boolean
    get() = openInventory.topInventory.type != InventoryType.CRAFTING

class VoidingVirtualInventory(size: Int) : VirtualInventory(null, size) {
    override fun setItemStackSilently(slot: Int, itemStack: ItemStack?) = Unit
    override fun forceSetItemStack(updateReason: UpdateReason?, slot: Int, itemStack: ItemStack?) = true
    override fun setItemStack(updateReason: UpdateReason?, slot: Int, itemStack: ItemStack?) = true
    override fun putItemStack(updateReason: UpdateReason?, slot: Int, itemStack: ItemStack) = 0
    override fun setItemAmount(updateReason: UpdateReason?, slot: Int, amount: Int) = amount
    override fun addItemAmount(updateReason: UpdateReason?, slot: Int, amount: Int) = amount
    override fun addItem(updateReason: UpdateReason?, itemStack: ItemStack?) = 0
    override fun collectToCursor(updateReason: UpdateReason?, itemStack: ItemStack?) = 0
    override fun simulateAdd(itemStacks: MutableList<ItemStack>) = IntArray(itemStacks.size)
    override fun simulateAdd(itemStack: ItemStack, vararg itemStacks: ItemStack) = IntArray(1 + itemStacks.size)
    override fun canHold(itemStacks: MutableList<ItemStack>) = true
}
