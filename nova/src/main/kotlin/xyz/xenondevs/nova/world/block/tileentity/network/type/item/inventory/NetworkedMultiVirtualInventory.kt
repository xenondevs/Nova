@file:Suppress("UNCHECKED_CAST")

package xyz.xenondevs.nova.world.block.tileentity.network.type.item.inventory

import org.bukkit.inventory.ItemStack
import xyz.xenondevs.invui.inventory.OperationCategory
import xyz.xenondevs.invui.inventory.VirtualInventory
import xyz.xenondevs.invui.inventory.event.UpdateReason
import xyz.xenondevs.nova.util.item.takeUnlessEmpty
import xyz.xenondevs.nova.world.block.tileentity.network.type.NetworkConnectionType
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.inventory.NetworkedVirtualInventory.Companion.UPDATE_REASON
import java.util.*

internal class NetworkedMultiVirtualInventory(
    override val uuid: UUID,
    inventories: Map<VirtualInventory, NetworkConnectionType>
) : NetworkedInventory {
    
    override val size: Int = inventories.keys.sumOf(VirtualInventory::getSize)
    
    val inventories: Map<VirtualInventory, NetworkConnectionType> =
        inventories.entries
            .sortedBy { (inv, _) -> inv.getGuiPriority(OperationCategory.ADD) }
            .associate { it.toPair() }
    
    private val inventoryBySlot: Array<VirtualInventory>
    private val invSlotBySlot: IntArray
    
    init {
        val inventoryBySlot = Array<VirtualInventory?>(size) { null }
        val invSlotBySlot = IntArray(size)
        
        var slot = 0
        for (inventory in this.inventories.keys) {
            for (invSlot in 0..<inventory.size) {
                inventoryBySlot[slot] = inventory
                invSlotBySlot[slot] = invSlot
                slot++
            }
        }
        
        this.inventoryBySlot = inventoryBySlot as Array<VirtualInventory>
        this.invSlotBySlot = invSlotBySlot
    }
    
    override fun add(itemStack: ItemStack, amount: Int): Int {
        var amountLeft = amount
        for ((inv, conType) in inventories) {
            if (!conType.insert)
                continue
            
            val itemStackWithAmount = itemStack.clone().also { it.amount = amountLeft }
            amountLeft = inv.addItem(UPDATE_REASON, itemStackWithAmount)
            if (amountLeft <= 0)
                break
        }
        
        return amountLeft
    }
    
    override fun canTake(slot: Int, amount: Int): Boolean {
        val inv = inventoryBySlot[slot]
        if (inv.preUpdateHandlers.isEmpty())
            return true
        val invSlot = invSlotBySlot[slot]
        
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
        val inv = inventoryBySlot[slot]
        val invSlot = invSlotBySlot[slot]
        
        val prev = inv.getItem(invSlot) ?: return
        inv.addItemAmount(UpdateReason.SUPPRESSED, invSlot, -amount)
        val post = inv.getItem(invSlot)
        inv.callPostUpdateEvent(UPDATE_REASON, invSlot, prev, post)
    }
    
    override fun isFull(): Boolean {
        return inventories.keys.all(VirtualInventory::isFull)
    }
    
    override fun isEmpty(): Boolean {
        return inventories.keys.all(VirtualInventory::isEmpty)
    }
    
    override fun copyContents(destination: Array<ItemStack>) {
        var invStartIdx = 0
        for ((inv, conType) in inventories) {
            if (conType.extract) {
                for ((idx, itemStack) in inv.unsafeItems.withIndex()) {
                    destination[invStartIdx + idx] = itemStack?.clone() ?: ItemStack.empty()
                }
            }
            
            invStartIdx += inv.size
        }
    }
    
    override fun canExchangeItemsWith(other: NetworkedInventory): Boolean {
        if (this == other)
            return false
        
        if (other is NetworkedVirtualInventory && other.virtualInventory in inventories)
            return false
        
        if (other is NetworkedMultiVirtualInventory && other.inventories.keys.any { it in inventories })
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