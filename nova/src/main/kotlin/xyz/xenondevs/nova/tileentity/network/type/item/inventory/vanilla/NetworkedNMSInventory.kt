package xyz.xenondevs.nova.tileentity.network.type.item.inventory.vanilla

import xyz.xenondevs.nova.tileentity.network.type.item.inventory.NetworkedInventory
import xyz.xenondevs.nova.util.bukkitCopy
import xyz.xenondevs.nova.util.item.takeUnlessEmpty
import xyz.xenondevs.nova.util.nmsVersion
import java.util.*
import kotlin.math.min
import net.minecraft.world.item.ItemStack as MojangStack
import org.bukkit.inventory.ItemStack as BukkitStack

internal open class NetworkedNMSInventory(
    private val container: MojangStackContainer
) : NetworkedInventory {
    
    // UUID is not required for vanilla item holder implementations, because inventory side configuration cannot be changed
    override val uuid = UUID(0L, 0L)
    
    override val size = container.size
    override val items: Array<BukkitStack?>
        get() = Array(container.size) { container[it].bukkitCopy.takeUnlessEmpty() }
    
    override fun addItem(item: BukkitStack): Int =
        addItem(item.nmsVersion)
    
    private fun addItem(itemStack: MojangStack): Int {
        val maxStackSize = itemStack.maxStackSize
        var remaining = itemStack.count
        
        // add to partial stacks
        for (current in container) {
            if (remaining <= 0)
                break
            
            if (!MojangStack.isSameItemSameTags(itemStack, current))
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
    
    override fun setItem(slot: Int, item: BukkitStack?): Boolean {
        container[slot] = item.nmsVersion
        return true
    }
    
    override fun decrementByOne(slot: Int) {
        container[slot].count--
    }
    
    override fun isFull(): Boolean {
        for (item in container) {
            if (item.isEmpty || item.count < item.maxStackSize)
                return false
        }
        
        return true
    }
    
}