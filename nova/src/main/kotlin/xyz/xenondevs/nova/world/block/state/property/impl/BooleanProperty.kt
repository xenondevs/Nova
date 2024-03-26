package xyz.xenondevs.nova.world.block.state.property.impl

import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.nova.world.block.state.property.BlockStateProperty
import xyz.xenondevs.nova.world.block.state.property.BlockStatePropertyInitializer
import xyz.xenondevs.nova.world.block.state.property.ScopedBlockStateProperty

class BooleanProperty(id: ResourceLocation) : BlockStateProperty<Boolean>(id) {
    
    override fun scope(values: Set<Boolean>, initializer: BlockStatePropertyInitializer<Boolean>): ScopedBlockStateProperty<Boolean> {
        return ScopedBooleanProperty(this, initializer)
    }
    
}

internal class ScopedBooleanProperty(
    property: BooleanProperty,
    initializer: BlockStatePropertyInitializer<Boolean>
) : ScopedBlockStateProperty<Boolean>(property, SET, initializer) {
    
    override fun isValidValue(value: Boolean): Boolean = true
    override fun isValidId(id: Int): Boolean = id in 0..1
    override fun isValidString(string: String): Boolean = string == "true" || string == "false"
    override fun valueToId(value: Boolean): Int = if (value) 1 else 0
    
    override fun idToValue(id: Int): Boolean = when (id) {
        0 -> false
        1 -> true
        else -> throw IllegalArgumentException("Id $id is not valid for property $id")
    }
    
    override fun stringToValue(string: String): Boolean = string.toBooleanStrict()
    override fun valueToString(value: Boolean): String = value.toString()
    
    companion object {
        private val SET = setOf(false, true)
    }
    
}