package xyz.xenondevs.nova.tileentity.network.type.item.inventory

import org.bukkit.inventory.ItemStack
import xyz.xenondevs.invui.inventory.Inventory
import xyz.xenondevs.invui.inventory.VirtualInventory
import xyz.xenondevs.invui.inventory.event.UpdateReason
import xyz.xenondevs.nova.util.item.takeUnlessEmpty
import java.util.*

open class NetworkedInvUIInventory(
    override val uuid: UUID, 
    private val inventory: Inventory, 
    private val updateReason: UpdateReason
) : NetworkedInventory {
    
    override val size: Int
        get() = inventory.size
    
    override val items: Array<ItemStack?>
        get() = inventory.items
    
    override fun setItem(slot: Int, item: ItemStack?): Boolean {
        return inventory.forceSetItem(null, slot, item)
    }
    
    override fun getItem(slot: Int): ItemStack? {
        return inventory.getItem(slot)
    }
    
    override fun addItem(item: ItemStack): Int {
        return inventory.addItem(updateReason, item)
    }
    
    override fun canDecrementByOne(slot: Int): Boolean {
        val itemStack = inventory.getUnsafeItem(slot) ?: return false
        val event = inventory.callPreUpdateEvent(
            updateReason,
            slot,
            itemStack.clone(),
            itemStack.clone().apply { amount-- }.takeUnlessEmpty()
        )
        
        return !event.isCancelled && (event.newItem?.amount ?: 0) == itemStack.amount - 1
    }
    
    override fun decrementByOne(slot: Int) {
        inventory.addItemAmount(UpdateReason.SUPPRESSED, slot, -1)
        
        val itemStack = inventory.getUnsafeItem(slot) ?: return
        inventory.callPostUpdateEvent(
            updateReason,
            slot,
            itemStack.clone(),
            itemStack.clone().apply { amount-- }.takeUnlessEmpty()
        )
    }
    
    override fun isFull(): Boolean {
        return inventory.isFull
    }
    
    override fun canExchangeItemsWith(other: NetworkedInventory): Boolean {
        return this != other && (other !is NetworkedMultiVirtualInventory || other.inventories.none { it.uuid == uuid })
    }
    
    override fun equals(other: Any?) =
        if (other is NetworkedInvUIInventory) other.uuid == uuid else false
    
    override fun hashCode() = uuid.hashCode()
    
}

/**
 * A [NetworkedInventory] wrapper for [VirtualInventory]
 */
class NetworkedVirtualInventory(
    val virtualInventory: VirtualInventory
) : NetworkedInvUIInventory(virtualInventory.uuid, virtualInventory, UPDATE_REASON) {
    
    companion object {
        
        /**
         * The [UpdateReason] used for [VirtualInventory] updates caused by item networks.
         */
        val UPDATE_REASON = object : UpdateReason {}
        
    }
    
}