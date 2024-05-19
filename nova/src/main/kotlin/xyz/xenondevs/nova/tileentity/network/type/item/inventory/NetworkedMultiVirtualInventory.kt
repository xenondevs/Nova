package xyz.xenondevs.nova.tileentity.network.type.item.inventory

import net.minecraft.world.item.ItemStack
import xyz.xenondevs.invui.inventory.VirtualInventory
import xyz.xenondevs.invui.inventory.event.UpdateReason
import xyz.xenondevs.nova.tileentity.network.type.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.type.item.inventory.NetworkedVirtualInventory.Companion.UPDATE_REASON
import xyz.xenondevs.nova.util.bukkitMirror
import xyz.xenondevs.nova.util.item.takeUnlessEmpty
import xyz.xenondevs.nova.util.nmsCopy
import xyz.xenondevs.nova.util.nmsVersion
import java.util.*

internal class NetworkedMultiVirtualInventory(
    override val uuid: UUID,
    inventories: Map<VirtualInventory, NetworkConnectionType>
) : NetworkedInventory {
    
    val inventories: List<VirtualInventory> =
        inventories.keys.sortedByDescending { it.guiPriority }
    
    private val invToConType: Map<VirtualInventory, NetworkConnectionType> =
        TreeMap<VirtualInventory, NetworkConnectionType>(compareByDescending { it.guiPriority })
            .apply { putAll(inventories) }
    
    override val size: Int
        get() = inventories.sumOf(VirtualInventory::getSize)
    
    override fun get(slot: Int): ItemStack {
        val (inv, invSlot) = getSlot(slot)
        return inv.getUnsafeItem(invSlot).nmsVersion
    }
    
    override fun set(slot: Int, itemStack: ItemStack) {
        val (inv, invSlot) = getSlot(slot)
        inv.forceSetItem(UPDATE_REASON, invSlot, itemStack.bukkitMirror)
    }
    
    override fun add(itemStack: ItemStack, amount: Int): Int {
        var amountLeft = itemStack.count
        for ((inv, conType) in invToConType) {
            if (!conType.insert)
                continue
            
            val itemStackWithCount = itemStack.copyWithCount(amountLeft).bukkitMirror
            amountLeft = inv.addItem(UPDATE_REASON, itemStackWithCount)
            if (amountLeft <= 0)
                break
        }
        
        return amountLeft
    }
    
    override fun canTake(slot: Int, amount: Int): Boolean {
        val (inv, invSlot) = getSlot(slot)
        if (inv.preUpdateHandler == null)
            return true
        
        val itemStack = inv.getUnsafeItem(invSlot) ?: return true
        val newAmount = itemStack.amount - amount
        val event = inv.callPreUpdateEvent(
            UPDATE_REASON,
            invSlot,
            itemStack.clone(),
            itemStack.clone().also { it.amount = newAmount }.takeUnlessEmpty()
        )
        
        return !event.isCancelled && (event.newItem?.amount ?: 0) == newAmount
    }
    
    override fun take(slot: Int, amount: Int) {
        val (inv, invSlot) = getSlot(slot)
        val prev = inv.getItem(invSlot) ?: return
        inv.addItemAmount(UpdateReason.SUPPRESSED, invSlot, -amount)
        val post = inv.getItem(invSlot)
        inv.callPostUpdateEvent(UPDATE_REASON, invSlot, prev, post)
    }
    
    override fun isFull(): Boolean {
        return inventories.all(VirtualInventory::isFull)
    }
    
    override fun isEmpty(): Boolean {
        return inventories.all(VirtualInventory::isEmpty)
    }
    
    override fun copyContents(destination: Array<ItemStack>) {
        var invStartIdx = 0
        for ((inv, conType) in invToConType) {
            if (conType.extract) {
                for ((idx, itemStack) in inv.unsafeItems.withIndex()) {
                    destination[invStartIdx + idx] = itemStack.nmsCopy
                }
            }
            
            invStartIdx += inv.size
        }
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
        return this === other || (other is NetworkedMultiVirtualInventory && inventories == other.inventories)
    }
    
    override fun hashCode(): Int {
        var result = 1
        inventories.forEach { result = 31 * result + it.hashCode() }
        return result
    }
    
}