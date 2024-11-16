package xyz.xenondevs.nova.world.block.tileentity.network.type.item.inventory.vanilla

import net.minecraft.world.level.block.entity.CrafterBlockEntity
import xyz.xenondevs.nova.util.unwrap
import net.minecraft.world.item.ItemStack as MojangStack
import org.bukkit.inventory.ItemStack as BukkitStack

internal class NetworkedCrafterInventory(
    private val entity: CrafterBlockEntity,
    container: ItemStackContainer
) : NetworkedNMSInventory(container) {
    
    override fun add(itemStack: BukkitStack, amount: Int): Int =
        add(itemStack.unwrap(), amount)
    
    private fun add(itemStack: MojangStack, amount: Int): Int {
        val maxStackSize = itemStack.maxStackSize
        var remaining = amount
        outer@ while (remaining > 0) {
            var bestPartial: MojangStack? = null
            for ((slot, slotStack) in container.withIndex()) {
                if (entity.isSlotDisabled(slot))
                    continue
                
                if (slotStack.isEmpty) {
                    container[slot] = itemStack.copyWithCount(1)
                    remaining--
                    continue@outer
                }
                
                if (MojangStack.isSameItemSameComponents(itemStack, slotStack)
                    && (bestPartial == null || slotStack.count < bestPartial.count)
                    && slotStack.count < maxStackSize
                ) {
                    bestPartial = slotStack
                }
            }
            
            // no more slots available
            if (bestPartial == null)
                return remaining
            
            bestPartial.count++
            remaining--
        }
        
        return 0
    }
    
}