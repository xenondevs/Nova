package xyz.xenondevs.nova.world.block.state

import xyz.xenondevs.nova.data.resources.lookup.ResourceLookups
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.block.state.model.LinkedBlockModelProvider
import xyz.xenondevs.nova.world.block.state.property.BlockStateProperty
import xyz.xenondevs.nova.world.block.state.property.PropertiesTree
import xyz.xenondevs.nova.world.block.state.property.ScopedBlockStateProperty

/**
 * A block state of a custom Nova block. Every block state is a unique configuration of a block's properties.
 */
open class NovaBlockState internal constructor(
    val block: NovaBlock,
    private val path: IntArray,
    private val scopedValues: Map<ScopedBlockStateProperty<*>, Any>
) {
    
    /**
     * The property-value configuration of this block state.
     */
    val values: Map<BlockStateProperty<*>, Any> = scopedValues.mapKeysTo(HashMap()) { it.key.property }
    
    /**
     * The [BlockStateProperties][BlockStateProperty] of this block state.
     */
    val properties: Set<BlockStateProperty<*>>
        get() = values.keys
    
    internal var tree: PropertiesTree<NovaBlockState>? = null
        private set
    
    internal open val modelProvider: LinkedBlockModelProvider<*> by lazy { ResourceLookups.BLOCK_MODEL[this]!! }
    
    /**
     * Gets the value of the given [property] or `null` if the property is not set.
     */
    @Suppress("UNCHECKED_CAST")
    operator fun <T : Any> get(property: BlockStateProperty<T>): T? {
        return values[property] as T?
    }
    
    /**
     * Gets the value of the given [property] or throws an [IllegalStateException] if the property is not set.
     */
    fun <T : Any> getOrThrow(property: BlockStateProperty<T>): T {
        return get(property) ?: throw IllegalStateException("$block does not have property $property")
    }
    
    /**
     * Gets the [NovaBlockState] for this [block] where [property] is [value] or throws an exception for invalid values.
     */
    fun <T : Any> with(property: BlockStateProperty<T>, value: T): NovaBlockState {
        val tree = tree ?: throw IllegalStateException("Block state has no properties tree")
        val (nodeIdx, scopedProperty) = tree.find(property)
        return tree.get(path, nodeIdx, scopedProperty.valueToId(value))
    }
    
    /**
     * Gets the [NovaBlockState] for this block with the given [propertyValues] or throws an exception for invalid values.
     */
    fun with(propertyValues: Map<BlockStateProperty<*>, Any>): NovaBlockState {
        if (propertyValues.isEmpty())
            return this
        val tree = tree ?: throw IllegalStateException("Block state has no properties tree")
        return tree.get(propertyValues)
    }
    
    /**
     * Gets the [NovaBlockState] with [property] cycled to the next value.
     */
    fun cycle(property: BlockStateProperty<*>): NovaBlockState {
        val tree = tree ?: throw IllegalStateException("Block state has no properties tree")
        val (depth, scopedProperty) = tree.find(property)
        val valueId = (path[depth] + 1) % scopedProperty.values.size
        return tree.get(path, depth, valueId)
    }
    
    @Suppress("UNCHECKED_CAST")
    override fun toString(): String {
        val propertiesStr = scopedValues.entries.joinToString { (property, value) ->
            val valStr = (property as ScopedBlockStateProperty<Any>).valueToString(value)
            "$property=$valStr"
        }
        
        return "$block[$propertiesStr]"
    }
    
    companion object {
        
        /**
         * Creates all possible [BlockStates][NovaBlockState] for the given [NovaBlock].
         */
        internal fun createBlockStates(
            block: NovaBlock,
            properties: List<ScopedBlockStateProperty<*>>,
        ): List<NovaBlockState> {
            if (properties.isNotEmpty()) {
                val tree = PropertiesTree(properties) { path, values -> NovaBlockState(block, path, values) }
                for (blockState in tree.elements)
                    blockState.tree = tree
                
                return tree.elements
            } else {
                return listOf(NovaBlockState(block, IntArray(0), emptyMap()))
            }
        }
        
    }
    
}