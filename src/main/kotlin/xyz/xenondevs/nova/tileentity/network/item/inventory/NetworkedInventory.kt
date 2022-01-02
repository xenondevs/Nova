package xyz.xenondevs.nova.tileentity.network.item.inventory

import de.studiocode.invui.virtualinventory.VirtualInventory
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.tileentity.TileEntityManager
import xyz.xenondevs.nova.tileentity.TileInventoryManager
import xyz.xenondevs.nova.tileentity.network.NetworkException
import xyz.xenondevs.nova.util.addItemCorrectly

private val UPDATE_REASON = CustomUpdateReason("NetworkedVirtualInventory")

private fun Array<ItemStack?>.deepClone() =
    Array(size) { get(it)?.clone() }

interface NetworkedInventory {
    
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
     */
    fun setItem(slot: Int, item: ItemStack?)
    
    /**
     * Gets the [ItemStack] on a specific slot.
     */
    fun getItem(slot: Int) = items[slot]
    
    /**
     * Decrements the amount of an [ItemStack] on a [slot] by one.
     */
    fun decrementByOne(slot: Int)
    
}

/**
 * A [NetworkedInventory] wrapper for [VirtualInventory]
 */
class NetworkedVirtualInventory(val virtualInventory: VirtualInventory) : NetworkedInventory {
    
    override val size: Int
        get() = virtualInventory.size
    
    override val items: Array<ItemStack?>
        get() = virtualInventory.items
    
    override fun setItem(slot: Int, item: ItemStack?) {
        if (!virtualInventory.setItemStack(null, slot, item))
            throwNetworkException()
    }
    
    override fun addItem(item: ItemStack): Int {
        return virtualInventory.addItem(UPDATE_REASON, item)
    }
    
    override fun decrementByOne(slot: Int) {
        if (virtualInventory.addItemAmount(UPDATE_REASON, slot, -1) != -1)
            throwNetworkException()
    }
    
    override fun equals(other: Any?) =
        if (other is NetworkedVirtualInventory) other.virtualInventory.uuid == virtualInventory.uuid else false
    
    override fun hashCode() = virtualInventory.uuid.hashCode()
    
    private fun throwNetworkException() {
        val uuid = virtualInventory.uuid
        val tileEntity = TileInventoryManager.getByUuid(uuid)?.first?.let { tileUUID ->
            TileEntityManager.tileEntities.firstOrNull { tileUUID == it.uuid }
        }
        throw NetworkException("The ItemUpdateEvent was cancelled. UUID: ${virtualInventory.uuid}, TileEntity: $tileEntity")
    }
    
}

/**
 * A [NetworkedInventory] wrapper for [Inventory]
 */
open class NetworkedBukkitInventory(val inventory: Inventory) : NetworkedInventory {
    
    override val size = inventory.size
    override val items: Array<ItemStack?>
        get() = inventory.contents.deepClone()
    
    override fun setItem(slot: Int, item: ItemStack?) {
        inventory.setItem(slot, item)
    }
    
    override fun addItem(item: ItemStack): Int {
        return inventory.addItemCorrectly(item)
    }
    
    override fun decrementByOne(slot: Int) {
        val item = inventory.getItem(slot)
        if (item != null) item.amount -= 1
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
class NetworkedChestInventory(inventory: Inventory) : NetworkedBukkitInventory(inventory) {
    
    override fun equals(other: Any?) =
        other is NetworkedChestInventory && other.inventory.location == inventory.location
    
    override fun hashCode() =
        inventory.location.hashCode()
    
}

/**
 * A [NetworkedInventory] specifically for shulker boxes. This implementation prevents the insertion
 * of other shulker boxes into the shulker box inventory.
 */
class NetworkedShulkerBoxInventory(inventory: Inventory) : NetworkedBukkitInventory(inventory) {
    
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
class NetworkedRangedBukkitInventory(
    private val inventory: Inventory,
    private vararg val slots: Int
) : NetworkedInventory {
    
    override val size = slots.size
    
    override val items: Array<ItemStack?>
        get() = inventory.contents.takeIndices(slots).deepClone()
    
    override fun setItem(slot: Int, item: ItemStack?) {
        inventory.setItem(slots[slot], item)
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
