package xyz.xenondevs.nova.tileentity.network.item.inventory

import de.studiocode.invui.virtualinventory.VirtualInventory
import de.studiocode.invui.virtualinventory.event.UpdateReason
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.commons.collections.flatMap
import xyz.xenondevs.nova.tileentity.TileEntityManager
import xyz.xenondevs.nova.tileentity.network.EndPointContainer
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.NetworkException
import xyz.xenondevs.nova.tileentity.network.item.inventory.NetworkedVirtualInventory.Companion.UPDATE_REASON
import xyz.xenondevs.nova.util.addItemCorrectly
import xyz.xenondevs.nova.util.isFull
import xyz.xenondevs.nova.util.item.takeUnlessEmpty

private fun Array<ItemStack?>.deepClone() =
    Array(size) { get(it)?.clone() }

interface NetworkedInventory : EndPointContainer {
    
    /**
     * How many slots the inventory has.
     */
    val size: Int
    
    /**
     * A copy of all the [ItemStack]s in this inventory.
     */
    val items: Array<ItemStack?>
    
    /**
     * Adds an [ItemStack] to the inventory and returns
     * how many items have been left over.
     */
    fun addItem(item: ItemStack): Int
    
    /**
     * Changes the [ItemStack] on a specific slot to the
     * specified [ItemStack].
     * @return If the action was successful
     */
    fun setItem(slot: Int, item: ItemStack?): Boolean
    
    /**
     * Gets the [ItemStack] on a specific slot.
     */
    fun getItem(slot: Int) = items[slot]
    
    /**
     * If the amount of the [ItemStack] on that [slot] can be decremented by one.
     */
    fun canDecrementByOne(slot: Int): Boolean = true
    
    /**
     * Decrements the amount of an [ItemStack] on a [slot] by one.
     */
    fun decrementByOne(slot: Int)
    
    /**
     * If all slots of this inventory are filled up to their max stack size
     */
    fun isFull(): Boolean
    
    /**
     * If this inventory is allowed to exchange items with [other].
     */
    fun canExchangeItemsWith(other: NetworkedInventory): Boolean {
        return this != other
    }
    
}

/**
 * A [NetworkedInventory] wrapper for [VirtualInventory]
 */
class NetworkedVirtualInventory internal constructor(val virtualInventory: VirtualInventory) : NetworkedInventory {
    
    override val size: Int
        get() = virtualInventory.size
    
    override val items: Array<ItemStack?>
        get() = virtualInventory.items
    
    override fun setItem(slot: Int, item: ItemStack?): Boolean {
        return virtualInventory.setItemStack(null, slot, item)
    }
    
    override fun addItem(item: ItemStack): Int {
        return virtualInventory.addItem(UPDATE_REASON, item)
    }
    
    override fun canDecrementByOne(slot: Int): Boolean {
        val itemStack = virtualInventory.getUnsafeItemStack(slot)
        val event = virtualInventory.callPreUpdateEvent(
            UPDATE_REASON,
            slot,
            itemStack,
            itemStack?.clone()?.apply { amount-- }?.takeUnlessEmpty()
        )
        
        return !event.isCancelled && (event.newItemStack?.amount ?: 0) == itemStack.amount - 1
    }
    
    override fun decrementByOne(slot: Int) {
        if(virtualInventory.addItemAmount(UpdateReason.SUPPRESSED, slot, -1) != -1)
            throwNetworkException()
        
        val itemStack = virtualInventory.getUnsafeItemStack(slot)
        virtualInventory.callAfterUpdateEvent(
            UPDATE_REASON,
            slot,
            itemStack,
            itemStack?.clone()?.apply { amount-- }?.takeUnlessEmpty()
        )
    }
    
    override fun isFull(): Boolean {
        return virtualInventory.isFull()
    }
    
    override fun canExchangeItemsWith(other: NetworkedInventory): Boolean {
        return this != other && (other !is NetworkedMultiVirtualInventory || other.inventories.none { it.uuid == virtualInventory.uuid })
    }
    
    override fun equals(other: Any?) =
        if (other is NetworkedVirtualInventory) other.virtualInventory.uuid == virtualInventory.uuid else false
    
    override fun hashCode() = virtualInventory.uuid.hashCode()
    
    private fun throwNetworkException() {
        val uuid = virtualInventory.uuid
        val tileEntity = TileEntityManager.tileEntities.first { tileEntity -> tileEntity.inventories.any { it.uuid == uuid } }
        throw NetworkException("The ItemUpdateEvent was cancelled. UUID: ${virtualInventory.uuid}, TileEntity: $tileEntity")
    }
    
    companion object {
        
        /**
         * The [UpdateReason] used for [VirtualInventory] updates caused by item networks.
         */
        val UPDATE_REASON = object : UpdateReason {}
        
    }
    
}

internal class NetworkedMultiVirtualInventory(inventories: Iterable<Pair<VirtualInventory, NetworkConnectionType>>) : NetworkedInventory {
    
    val inventories: List<VirtualInventory> =
        inventories.map { it.first }.sortedByDescending { it.guiShiftPriority }
    
    private val inventoriesWithConnectionType: List<Pair<VirtualInventory, NetworkConnectionType>> =
        inventories.sortedByDescending { it.first.guiShiftPriority }
    
    override val size: Int
        get() = inventories.sumOf { it.size }
    
    override val items: Array<ItemStack?>
        get() = inventoriesWithConnectionType.flatMap { (inv, conType) -> if (conType.extract) inv.items else arrayOfNulls(inv.size) }.toTypedArray()
    
    override fun setItem(slot: Int, item: ItemStack?): Boolean {
        val (inv, invSlot) = getSlot(slot)
        return inv.setItemStack(UPDATE_REASON, invSlot, item)
    }
    
    override fun addItem(item: ItemStack): Int {
        var amountLeft = item.amount
        for ((inv, conType) in inventoriesWithConnectionType) {
            if (!conType.insert)
                continue
            amountLeft = inv.addItem(UPDATE_REASON, item)
            if (amountLeft == 0)
                break
            item.amount = amountLeft
        }
        
        return amountLeft
    }
    
    override fun canDecrementByOne(slot: Int): Boolean {
        val (inv, invSlot) = getSlot(slot)
        val itemStack = inv.getUnsafeItemStack(invSlot)
        val event = inv.callPreUpdateEvent(
            UPDATE_REASON,
            invSlot,
            itemStack,
            itemStack?.clone()?.apply { amount-- }?.takeUnlessEmpty()
        )
        
        return !event.isCancelled && (event.newItemStack?.amount ?: 0) == itemStack.amount - 1
    }
    
    override fun decrementByOne(slot: Int) {
        val (inv, invSlot) = getSlot(slot)
        if (inv.addItemAmount(UpdateReason.SUPPRESSED, invSlot, -1) != -1)
            throwNetworkException(inv)
        
        inv.callAfterUpdateEvent(
            UPDATE_REASON,
            invSlot,
            inv.getUnsafeItemStack(invSlot),
            inv.getUnsafeItemStack(invSlot)?.clone()?.apply { amount-- }?.takeUnlessEmpty()
        )
    }
    
    override fun isFull(): Boolean {
        return inventories.all(VirtualInventory::isFull)
    }
    
    override fun canExchangeItemsWith(other: NetworkedInventory): Boolean {
        if (this == other)
            return false
        
        if (other is NetworkedVirtualInventory && inventories.any { it.uuid == other.virtualInventory.uuid })
            return false
        
        if (other is NetworkedMultiVirtualInventory && inventories.any { myInv -> other.inventories.any { otherInv -> myInv.uuid == otherInv.uuid } })
            return false
        
        return true
    }
    
    override fun equals(other: Any?): Boolean {
        return other is NetworkedMultiVirtualInventory
            && inventories.size == other.inventories.size
            && inventories.withIndex().all { (idx, pair) -> pair.uuid == other.inventories[idx].uuid }
    }
    
    override fun hashCode(): Int {
        var result = 1
        inventories.forEach { result = 31 * result + it.uuid.hashCode() }
        return result
    }
    
    private fun getSlot(slot: Int): Pair<VirtualInventory, Int> {
        var invSlot = slot
        inventories.forEach { inv ->
            val size = inv.size
            if (invSlot < size) return inv to invSlot
            invSlot -= size
        }
        
        throw IndexOutOfBoundsException("Slot $slot is out of bounds for this inventories: $inventories")
    }
    
    private fun throwNetworkException(inv: VirtualInventory) {
        val uuid = inv.uuid
        val tileEntity = TileEntityManager.tileEntities.first { tileEntity -> tileEntity.inventories.any { it.uuid == uuid } }
        throw NetworkException("The ItemUpdateEvent was cancelled. UUID: ${inv.uuid}, TileEntity: $tileEntity")
    }
    
}

/**
 * A [NetworkedInventory] wrapper for [Inventory]
 */
internal open class NetworkedBukkitInventory(val inventory: Inventory) : NetworkedInventory {
    
    override val size = inventory.size
    override val items: Array<ItemStack?>
        get() = inventory.contents.deepClone()
    
    override fun setItem(slot: Int, item: ItemStack?): Boolean {
        inventory.setItem(slot, item)
        return true
    }
    
    override fun addItem(item: ItemStack): Int {
        return inventory.addItemCorrectly(item)
    }
    
    override fun decrementByOne(slot: Int) {
        val item = inventory.getItem(slot)
        if (item != null) item.amount -= 1
    }
    
    override fun isFull(): Boolean {
        return inventory.isFull()
    }
    
    override fun equals(other: Any?) =
        if (other is NetworkedBukkitInventory) other.inventory == inventory else false
    
    override fun hashCode() =
        inventory.hashCode()
    
}

/**
 * A [NetworkedInventory] specifically for chests. This implementation compares the inventory location
 * instead of the inventories themselves, so double chest inventories are seen as the same inventory.
 */
internal class NetworkedChestInventory(inventory: Inventory) : NetworkedBukkitInventory(inventory) {
    
    override fun equals(other: Any?) =
        other is NetworkedChestInventory && other.inventory.location == inventory.location
    
    override fun hashCode() =
        inventory.location.hashCode()
    
}

/**
 * A [NetworkedInventory] specifically for shulker boxes. This implementation prevents the insertion
 * of other shulker boxes into the shulker box inventory.
 */
internal class NetworkedShulkerBoxInventory(inventory: Inventory) : NetworkedBukkitInventory(inventory) {
    
    override fun addItem(item: ItemStack): Int {
        return if (item.type.name.contains("SHULKER_BOX")) item.amount
        else super.addItem(item)
    }
    
}

/**
 * A [NetworkedInventory] wrapper for specific slots of a [Inventory].
 *
 * Useful for splitting different slots inside vanilla TileEntities into multiple [NetworkedInventory]s
 * such as one for the input and one for the output of that TileEntity.
 */
internal class NetworkedRangedBukkitInventory(
    private val inventory: Inventory,
    private vararg val slots: Int
) : NetworkedInventory {
    
    override val size = slots.size
    
    override val items: Array<ItemStack?>
        get() = inventory.contents.takeIndices(slots).deepClone()
    
    override fun setItem(slot: Int, item: ItemStack?): Boolean {
        inventory.setItem(slots[slot], item)
        return true
    }
    
    override fun decrementByOne(slot: Int) {
        val item = inventory.getItem(slots[slot])
        if (item != null) item.amount -= 1
    }
    
    override fun addItem(item: ItemStack): Int {
        @Suppress("UNCHECKED_CAST")
        val tempInventory = VirtualInventory(null, size, items as Array<ItemStack>, null) // create a temp virtual inventory
        val amount = tempInventory.addItem(null, item) // add item to the temp inventory
        
        // copy items from temp inv to real inv
        for (slot in 0 until size) {
            inventory.setItem(slots[slot], tempInventory.getItemStack(slot))
        }
        
        return amount
    }
    
    override fun isFull(): Boolean {
        for (slot in slots) {
            val item = inventory.getItem(slot)
            if (item == null || item.amount < item.type.maxStackSize)
                return false
        }
        
        return true
    }
    
    override fun equals(other: Any?) =
        if (other is NetworkedRangedBukkitInventory) other.inventory == inventory && other.slots.contentEquals(slots) else false
    
    override fun hashCode(): Int {
        var result = inventory.hashCode()
        result = 31 * result + slots.contentHashCode()
        return result
    }
    
}

private inline fun <reified T> Array<T>.takeIndices(indices: IntArray) =
    indices.map { get(it) }.toTypedArray()
