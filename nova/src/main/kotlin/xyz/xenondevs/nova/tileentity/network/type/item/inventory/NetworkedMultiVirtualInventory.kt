package xyz.xenondevs.nova.tileentity.network.type.item.inventory

import org.bukkit.inventory.ItemStack
import xyz.xenondevs.commons.collections.flatMap
import xyz.xenondevs.invui.inventory.VirtualInventory
import xyz.xenondevs.invui.inventory.event.UpdateReason
import xyz.xenondevs.nova.tileentity.network.type.NetworkConnectionType
import xyz.xenondevs.nova.util.item.takeUnlessEmpty
import java.util.*

internal class NetworkedMultiVirtualInventory(
    override val uuid: UUID,
    inventories: Map<VirtualInventory, NetworkConnectionType>
) : NetworkedInventory {
    
    val inventories: List<VirtualInventory> =
        inventories.keys.sortedByDescending { it.guiPriority }
    
    private val inventoriesWithConnectionType: List<Map.Entry<VirtualInventory, NetworkConnectionType>> =
        inventories.entries.sortedByDescending { (vi, _) -> vi.guiPriority }
    
    override val size: Int
        get() = inventories.sumOf(VirtualInventory::getSize)
    
    override val items: Array<ItemStack?>
        get() = inventoriesWithConnectionType.flatMap { (inv, conType) -> if (conType.extract) inv.items else arrayOfNulls(inv.size) }.toTypedArray()
    
    override fun setItem(slot: Int, item: ItemStack?): Boolean {
        val (inv, invSlot) = getSlot(slot)
        return inv.forceSetItem(NetworkedInvUIInventory.UPDATE_REASON, invSlot, item)
    }
    
    override fun addItem(item: ItemStack): Int {
        var amountLeft = item.amount
        for ((inv, conType) in inventoriesWithConnectionType) {
            if (!conType.insert)
                continue
            amountLeft = inv.addItem(NetworkedInvUIInventory.UPDATE_REASON, item)
            if (amountLeft == 0)
                break
            item.amount = amountLeft
        }
        
        return amountLeft
    }
    
    override fun canDecrementByOne(slot: Int): Boolean {
        val (inv, invSlot) = getSlot(slot)
        val itemStack = inv.getUnsafeItem(invSlot) ?: return false
        val event = inv.callPreUpdateEvent(
            NetworkedInvUIInventory.UPDATE_REASON,
            invSlot,
            itemStack.clone(),
            itemStack.clone().apply { amount-- }.takeUnlessEmpty()
        )
        
        return !event.isCancelled && (event.newItem?.amount ?: 0) == itemStack.amount - 1
    }
    
    override fun decrementByOne(slot: Int) {
        val (inv, invSlot) = getSlot(slot)
        inv.addItemAmount(UpdateReason.SUPPRESSED, invSlot, -1)
        
        val itemStack = inv.getUnsafeItem(invSlot) ?: return
        inv.callPostUpdateEvent(
            NetworkedInvUIInventory.UPDATE_REASON,
            invSlot,
            itemStack.clone(),
            itemStack.clone().apply { amount-- }.takeUnlessEmpty()
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
    
}