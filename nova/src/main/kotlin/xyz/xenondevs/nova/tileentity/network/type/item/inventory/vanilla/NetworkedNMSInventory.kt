package xyz.xenondevs.nova.tileentity.network.type.item.inventory.vanilla

import net.minecraft.world.item.ItemStack
import xyz.xenondevs.nova.tileentity.network.type.item.inventory.NetworkedInventory
import java.util.*
import kotlin.math.min

internal open class NetworkedNMSInventory(
    private val container: ItemStackContainer
) : NetworkedInventory {
    
    // UUID is not required for vanilla item holder implementations, because inventory side configuration cannot be changed
    override val uuid = UUID(0L, 0L)
    override val size = container.size
    
    override fun add(itemStack: ItemStack, amount: Int): Int {
        val maxStackSize = itemStack.maxStackSize
        var remaining = amount
        
        // add to partial stacks
        for (current in container) {
            if (remaining <= 0)
                break
            
            if (!ItemStack.isSameItemSameComponents(itemStack, current))
                continue
            
            val transfer = min(remaining, maxStackSize - current.count)
            current.count += transfer
            remaining -= transfer
        }
        
        // add to empty slots
        for ((slot, current) in container.withIndex()) {
            if (remaining <= 0)
                break
            
            if (!current.isEmpty)
                continue
            
            val transfer = min(remaining, maxStackSize)
            container[slot] = itemStack.copy().apply { count = transfer }
            remaining -= transfer
        }
        
        return remaining
    }
    
    override fun canTake(slot: Int, amount: Int): Boolean {
        return true
    }
    
    override fun take(slot: Int, amount: Int) {
        val current = container[slot]
        if (current.isEmpty)
            return
        
        val transfer = min(amount, current.count)
        current.count -= transfer
    }
    
    override fun isFull(): Boolean {
        for (item in container) {
            if (item.isEmpty || item.count < item.maxStackSize)
                return false
        }
        
        return true
    }
    
    override fun isEmpty(): Boolean {
        return container.all(ItemStack::isEmpty)
    }
    
    override fun copyContents(destination: Array<ItemStack>) {
        for ((index, item) in container.withIndex()) {
            destination[index] = item.copy()
        }
    }
    
    override fun equals(other: Any?): Boolean {
        return this === other || other is NetworkedNMSInventory && container == other.container
    }
    
    override fun hashCode(): Int {
        return container.hashCode()
    }
    
}