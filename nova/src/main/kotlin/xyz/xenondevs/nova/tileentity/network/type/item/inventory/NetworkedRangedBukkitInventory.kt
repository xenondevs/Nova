package xyz.xenondevs.nova.tileentity.network.type.item.inventory

import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.invui.inventory.VirtualInventory
import xyz.xenondevs.nova.util.item.deepClone
import xyz.xenondevs.nova.util.item.takeIndices
import java.util.*

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
    
    // UUID is not required for vanilla item holder implementations, because inventory side configuration cannot be changed
    override val uuid = UUID(0L, 0L)
    
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
        for (slot in 0..<size) {
            inventory.setItem(slots[slot], tempInventory.getItem(slot))
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