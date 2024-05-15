package xyz.xenondevs.nova.tileentity.network.type.item.inventory.vanilla

import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ItemStack as MojangStack

internal interface MojangStackContainer : Iterable<MojangStack> {
    
    val size: Int
    
    operator fun get(index: Int): MojangStack
    
    operator fun set(index: Int, value: MojangStack)
    
}

internal class SingleMojangStackContainer(private val items: MutableList<MojangStack>) : MojangStackContainer {
    
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
    
}

internal class SingleSlotMojangStackContainer(private val items: MutableList<MojangStack>, private val slot: Int) : MojangStackContainer {
    
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
    
    private inner class IteratorImpl : Iterator<ItemStack> {
        
        private var hasNext = true
        
        override fun hasNext(): Boolean {
            return hasNext
        }
        
        override fun next(): ItemStack {
            if (!hasNext)
                throw NoSuchElementException()
            
            hasNext = false
            return items[0]
        }
        
    }
    
}

internal class DoubleMojangStackContainer(
    private val left: MutableList<MojangStack>,
    private val right: MutableList<MojangStack>
) : MojangStackContainer {
    
    override val size = left.size + right.size
    
    override fun get(index: Int): MojangStack {
        return if (index < left.size) left[index] else right[index - left.size]
    }
    
    override fun set(index: Int, value: MojangStack) {
        if (index < left.size) left[index] = value else right[index - left.size] = value
    }
    
    override fun iterator(): Iterator<MojangStack> {
        return IteratorImpl()
    }
    
    inner class IteratorImpl : Iterator<MojangStack> {
        
        private var i = -1
        
        override fun hasNext(): Boolean {
            return i + 1 < left.size + right.size
        }
        
        override fun next(): MojangStack {
            if (!hasNext())
                throw NoSuchElementException()
            
            return get(++i)
        }
        
    }
    
}