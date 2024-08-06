package xyz.xenondevs.nova.tileentity.network.type.item.inventory.vanilla

import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ItemStack as MojangStack

internal interface ItemStackContainer : Iterable<MojangStack> {
    
    val size: Int
    
    operator fun get(index: Int): MojangStack
    
    operator fun set(index: Int, value: MojangStack)
    
}

internal class SimpleItemStackContainer(private val items: MutableList<MojangStack>) : ItemStackContainer {
    
    override val size = items.size
    
    override fun get(index: Int): MojangStack {
        return items[index]
    }
    
    override fun set(index: Int, value: MojangStack) {
        items[index] = value
    }
    
    override fun iterator(): Iterator<MojangStack> {
        return items.iterator()
    }
    
    override fun equals(other: Any?): Boolean {
        return this === other || other is SimpleItemStackContainer && items === other.items
    }
    
    override fun hashCode(): Int {
        return System.identityHashCode(items)
    }
    
}

internal class SingleSlotItemStackContainer(private val items: MutableList<MojangStack>, private val slot: Int) : ItemStackContainer {
    
    override val size = 1
    
    override fun get(index: Int): ItemStack {
        if (index != 0)
            throw IndexOutOfBoundsException(index)
        
        return items[slot]
    }
    
    override fun set(index: Int, value: ItemStack) {
        if (index != 0)
            throw IndexOutOfBoundsException(index)
        
        items[slot] = value
    }
    
    override fun iterator(): Iterator<ItemStack> {
        return IteratorImpl()
    }
    
    override fun equals(other: Any?): Boolean {
        return this === other || other is SingleSlotItemStackContainer && items === other.items && slot == other.slot
    }
    
    override fun hashCode(): Int {
        var result = System.identityHashCode(items)
        result = 31 * result + slot
        return result
    }
    
    private inner class IteratorImpl : Iterator<ItemStack> {
        
        private var hasNext = true
        
        override fun hasNext(): Boolean {
            return hasNext
        }
        
        override fun next(): ItemStack {
            if (!hasNext)
                throw NoSuchElementException()
            
            hasNext = false
            return items[slot]
        }
        
    }
    
}

internal class DoubleChestItemStackContainer(
    private val left: MutableList<MojangStack>,
    private val right: MutableList<MojangStack>
) : ItemStackContainer {
    
    override val size = 54
    
    override fun get(index: Int): MojangStack {
        return if (index < 27) right[index] else left[index - 27]
    }
    
    override fun set(index: Int, value: MojangStack) {
        if (index < 27) right[index] = value else left[index - 27] = value
    }
    
    override fun iterator(): Iterator<MojangStack> {
        return IteratorImpl()
    }
    
    override fun equals(other: Any?): Boolean {
        return this === other || other is DoubleChestItemStackContainer && left === other.left && right === other.right
    }
    
    override fun hashCode(): Int {
        var result = System.identityHashCode(left)
        result = 31 * result + System.identityHashCode(right)
        return result
    }
    
    inner class IteratorImpl : Iterator<MojangStack> {
        
        private var i = -1
        
        override fun hasNext(): Boolean {
            return i + 1 < 54
        }
        
        override fun next(): MojangStack {
            if (!hasNext())
                throw NoSuchElementException()
            
            return get(++i)
        }
        
    }
    
}