package xyz.xenondevs.nova.world.block.tileentity.network.type.item.inventory.vanilla

import xyz.xenondevs.nova.util.asBukkitMirror
import net.minecraft.world.level.block.entity.BlockEntity
import xyz.xenondevs.nova.util.unwrap
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.inventory.NetworkedInventory
import java.util.*
import kotlin.math.min
import net.minecraft.world.item.ItemStack as MojangStack
import org.bukkit.inventory.ItemStack as BukkitStack

internal open class NetworkedNMSInventory(
    protected val container: ItemStackContainer,
    private vararg val blockEntities: BlockEntity
) : NetworkedInventory {
    
    // UUID is not required for vanilla item holder implementations, because inventory side configuration cannot be changed
    override val uuid = UUID(0L, 0L)
    override val size = container.size
    
    private var changed = false
    
    override fun add(itemStack: BukkitStack, amount: Int): Int {
        val maxStackSize = itemStack.maxStackSize
        var remaining = amount
        
        // add to partial stacks
        for (current in container) {
            if (remaining <= 0)
                break
            
            if (!MojangStack.isSameItemSameComponents(itemStack.unwrap(), current))
                continue
            
            val transfer = min(remaining, maxStackSize - current.count)
            current.count += transfer
            remaining -= transfer
        }
        
        // add to empty slots
        for ([slot, current] in container.withIndex()) {
            if (remaining <= 0)
                break
            
            if (!current.isEmpty)
                continue
            
            val transfer = min(remaining, maxStackSize)
            container[slot] = itemStack.unwrap().copyWithCount(transfer)
            remaining -= transfer
        }
        
        if (remaining < amount)
            markChanged()
        
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
        if (transfer > 0)
            markChanged()
    }
    
    override fun isFull(): Boolean {
        for (item in container) {
            if (item.isEmpty || item.count < item.maxStackSize)
                return false
        }
        
        return true
    }
    
    override fun isEmpty(): Boolean {
        return container.all(MojangStack::isEmpty)
    }
    
    override fun copyContents(destination: Array<BukkitStack>) {
        for ([index, item] in container.withIndex()) {
            destination[index] = item.copy().asBukkitMirror()
        }
    }
    
    protected fun markChanged() {
        changed = true
    }
    
    fun postNetworkTickSync() {
        if (!changed)
            return
        
        for (blockEntity in blockEntities) {
            blockEntity.setChanged()
        }
        changed = false
    }
    
    override fun equals(other: Any?): Boolean {
        return this === other || other is NetworkedNMSInventory && container == other.container
    }
    
    override fun hashCode(): Int {
        return container.hashCode()
    }
    
}
