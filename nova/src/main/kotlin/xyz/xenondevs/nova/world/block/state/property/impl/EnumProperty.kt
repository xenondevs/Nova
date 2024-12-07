package xyz.xenondevs.nova.world.block.state.property.impl

import net.kyori.adventure.key.Key
import xyz.xenondevs.nova.world.block.state.property.BlockStateProperty
import xyz.xenondevs.nova.world.block.state.property.BlockStatePropertyInitializer
import xyz.xenondevs.nova.world.block.state.property.ScopedBlockStateProperty
import java.util.*

inline fun <reified E : Enum<E>> EnumProperty(id: Key): EnumProperty<E> =
    EnumProperty(id, E::class.java)

class EnumProperty<E : Enum<E>>(
    id: Key,
    private val enumClass: Class<E>,
) : BlockStateProperty<E>(id) {
    
    override fun scope(values: Set<E>, initializer: BlockStatePropertyInitializer<E>): ScopedBlockStateProperty<E> {
        val scopeValues = values.ifEmpty { EnumSet.allOf(enumClass) }
        return ScopedEnumProperty(this, enumClass, scopeValues, initializer)
    }
    
}

internal class ScopedEnumProperty<E : Enum<E>>(
    property: EnumProperty<E>,
    enumClass: Class<E>,
    values: Set<E>,
    initializer: BlockStatePropertyInitializer<E>
) : ScopedBlockStateProperty<E>(property, EnumSet.copyOf(values), initializer) {
    
    private val idToValue: List<E> = values.toList()
    private val valueToId: Map<E, Int> = idToValue.withIndex().associateTo(EnumMap(enumClass)) { (index, value) -> value to index }
    private val stringToValue: Map<String, E> = idToValue.associateByTo(HashMap()) { it.name.lowercase() }
    
    override fun isValidValue(value: E): Boolean =
        value in valueToId
    
    override fun isValidId(id: Int): Boolean =
        id in idToValue.indices
    
    override fun isValidString(string: String): Boolean =
        string.lowercase() in stringToValue
    
    override fun valueToId(value: E): Int =
        valueToId[value] ?: throw IllegalArgumentException("Value $value is not valid for property $this")
    
    override fun idToValue(id: Int): E =
        idToValue.getOrNull(id) ?: throw IllegalArgumentException("Id $id is not valid for property $this")
    
    override fun stringToValue(string: String): E =
        stringToValue[string] ?: throw IllegalArgumentException("String $string is not valid for property $this")
    
    override fun valueToString(value: E): String =
        if (value in valueToId) value.name.lowercase() else throw IllegalArgumentException("Value $value is not valid for property $this")
    
}