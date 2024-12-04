package xyz.xenondevs.nova.world.block.state.property

import net.kyori.adventure.key.Key
import xyz.xenondevs.nova.context.Context
import xyz.xenondevs.nova.context.intention.DefaultContextIntentions.BlockPlace

/**
 * Represents a property-type of a block state.
 */
abstract class BlockStateProperty<T : Any>(val id: Key) {
    
    /**
     * Creates a new [ScopedBlockStateProperty] of this property that is limited to the given [values]
     * and initialized using the given [initializer].
     */
    fun scope(vararg values: T, initializer: BlockStatePropertyInitializer<T> = { values[0] }): ScopedBlockStateProperty<T> =
        scope(values.toHashSet(), initializer)
    
    /**
     * Creates a new [ScopedBlockStateProperty] of this property that is limited to the given [values]
     * and initialized using the given [initializer].
     */
    abstract fun scope(values: Set<T>, initializer: BlockStatePropertyInitializer<T>): ScopedBlockStateProperty<T>
    
    override fun toString(): String = id.toString()
    
}

internal typealias BlockStatePropertyInitializer<T> = (Context<BlockPlace>) -> T

/**
 * Represents a property-type of a block state, along with the allowed values.
 */
abstract class ScopedBlockStateProperty<T : Any>(
    val property: BlockStateProperty<T>,
    val values: Set<T>,
    val initializer: BlockStatePropertyInitializer<T>
) {
    
    /**
     * Determines whether the given [value] is valid for this property.
     */
    open fun isValidValue(value: T): Boolean = value in values
    
    /**
     * Determines whether the given [id] is valid for this property.
     */
    open fun isValidId(id: Int): Boolean = id in values.indices
    
    /**
     * Determines whether the given [string] is valid for this property.
     */
    abstract fun isValidString(string: String): Boolean
    
    /**
     * Converts the given [value] to its corresponding id
     * or throws an [IllegalArgumentException] if [value] is not valid for this property.
     */
    abstract fun valueToId(value: T): Int
    
    /**
     * Converts the given [id] to its corresponding value of type [T]
     * or throws an [IllegalArgumentException] if [id] is not valid for this property.
     */
    abstract fun idToValue(id: Int): T?
    
    /**
     * Converts the given [value] to a [String]
     * or throws an [IllegalArgumentException] if [value] is not valid for this property.
     */
    abstract fun valueToString(value: T): String
    
    /**
     * Converts the given [string] to the corresponding value
     * or throws an [IllegalArgumentException] if [string] is not valid for this property.
     */
    abstract fun stringToValue(string: String): T
    
    override fun toString(): String = property.toString()
    
}