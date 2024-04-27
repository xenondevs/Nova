package xyz.xenondevs.nova.tileentity.network.type.item.inventory

import org.bukkit.Location
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.invui.inventory.CompositeInventory
import xyz.xenondevs.invui.inventory.ReferencingInventory
import xyz.xenondevs.nova.util.addItemCorrectly
import xyz.xenondevs.nova.util.isFull
import xyz.xenondevs.nova.util.item.deepClone
import java.util.*

/**
 * A [NetworkedInventory] wrapper for [Inventory]
 */
internal open class NetworkedBukkitInventory(
    val inventory: Inventory
) : NetworkedInventory {
    
    // UUID is not required for vanilla item holder implementations, because inventory side configuration cannot be changed
    override val uuid = UUID(0L, 0L)
    
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

internal class NetworkedDoubleChestInventory(left: Inventory, right: Inventory) : NetworkedInvUIInventory(
    UUID(0L, 0L),
    CompositeInventory(
        ReferencingInventory.fromContents(right),
        ReferencingInventory.fromContents(left)
    )
) {
    
    private val leftLocation: Location = left.location ?: throw IllegalArgumentException("Missing left location")
    private val rightLocation: Location = right.location ?: throw IllegalArgumentException("Missing right location")
    
    override fun equals(other: Any?): Boolean {
        return other is NetworkedDoubleChestInventory
            && other.leftLocation == leftLocation
            && other.rightLocation == rightLocation
    }
    
    override fun hashCode(): Int {
        var result = leftLocation.hashCode()
        result = 31 * result + rightLocation.hashCode()
        return result
    }
    
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