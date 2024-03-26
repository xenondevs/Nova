package xyz.xenondevs.nova.world.block.state.property.impl

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.nova.world.block.state.property.BlockStateProperty
import xyz.xenondevs.nova.world.block.state.property.BlockStatePropertyInitializer
import xyz.xenondevs.nova.world.block.state.property.ScopedBlockStateProperty

class IntProperty(id: ResourceLocation) : BlockStateProperty<Int>(id) {
    
    fun scope(range: IntRange, initializer: BlockStatePropertyInitializer<Int> = { range.first }): ScopedBlockStateProperty<Int> {
        return scope(range.toSet(), initializer)
    }
    
    override fun scope(values: Set<Int>, initializer: BlockStatePropertyInitializer<Int>): ScopedBlockStateProperty<Int> {
        return ScopedIntProperty(this, values, initializer)
    }
    
}

internal class ScopedIntProperty(
    property: BlockStateProperty<Int>,
    values: Set<Int>,
    initializer: BlockStatePropertyInitializer<Int>
) : ScopedBlockStateProperty<Int>(property, IntOpenHashSet(values), initializer) {
    
    private val idToValue = values.toIntArray().apply(IntArray::sort)
    private val valueToId = values.withIndex().associateTo(Int2IntOpenHashMap()) { (index, value) -> value to index }
    
    override fun isValidValue(value: Int): Boolean =
        value in valueToId
    
    override fun isValidId(id: Int): Boolean =
        id in values.indices
    
    override fun isValidString(string: String): Boolean =
        string.toIntOrNull() in valueToId
    
    override fun valueToId(value: Int): Int {
        if (value !in valueToId)
            throw IllegalArgumentException("Value $value is not valid for property $this")
        
        return valueToId.get(value)
    }
    
    override fun idToValue(id: Int): Int {
        if (id !in values.indices)
            throw IllegalArgumentException("Id $id is not valid for property $id")
        
        return idToValue[id]
    }
    
    override fun stringToValue(string: String): Int {
        val num = string.toIntOrNull()
            ?: throw IllegalArgumentException("$string is not a valid integer")
        
        if (num !in valueToId)
            throw IllegalArgumentException("Value $num is not valid for property $this")
        
        return num
    }
    
    override fun valueToString(value: Int): String {
        if (value !in valueToId)
            throw IllegalArgumentException("Value $value is not valid for property $this")
        
        return value.toString()
    }
    
}