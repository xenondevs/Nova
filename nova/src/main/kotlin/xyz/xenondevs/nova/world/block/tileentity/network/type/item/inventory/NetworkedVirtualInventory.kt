package xyz.xenondevs.nova.world.block.tileentity.network.type.item.inventory

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
    
    override fun add(itemStack: ItemStack, amount: Int): Int {
        val itemStackWithAmount = itemStack.clone().also { it.amount = amount }
        return inventory.addItem(updateReason, itemStackWithAmount)
    }
    
    override fun canTake(slot: Int, amount: Int): Boolean {
        if (inventory.preUpdateHandler == null)
            return true
        
        val itemStack = inventory.getUnsafeItem(slot) ?: return true
        val newAmount = itemStack.amount - amount
        val event = inventory.callPreUpdateEvent(
            updateReason,
            slot,
            itemStack.clone(),
            itemStack.clone().also { it.amount = newAmount }.takeUnlessEmpty()
        )
        
        return !event.isCancelled && (event.newItem?.amount ?: 0) == newAmount
    }
    
    override fun take(slot: Int, amount: Int) {
        val prev = inventory.getItem(slot) ?: return
        inventory.addItemAmount(UpdateReason.SUPPRESSED, slot, -amount)
        val post = inventory.getItem(slot)
        inventory.callPostUpdateEvent(updateReason, slot, prev, post)
    }
    
    override fun isFull(): Boolean {
        return inventory.isFull
    }
    
    override fun isEmpty(): Boolean {
        return inventory.isEmpty
    }
    
    override fun copyContents(destination: Array<ItemStack>) {
        for ((slot, item) in inventory.unsafeItems.withIndex()) {
            destination[slot] = item?.clone() ?: ItemStack.empty()
        }
    }
    
    override fun canExchangeItemsWith(other: NetworkedInventory): Boolean {
        if (this == other)
            return false
        
        if (other is NetworkedMultiVirtualInventory && other.inventories.keys.any { it == inventory })
            return false
        
        return true
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other)
            return true
        
        if (other is NetworkedInvUIInventory)
            return inventory == other.inventory
        
        return false
    }
    
    override fun hashCode(): Int = inventory.hashCode()
    
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
    
    init {
        virtualInventory.addResizeHandler { _, _ ->
            throw UnsupportedOperationException("Networked inventories cannot be resized")
        }
    }
    
}