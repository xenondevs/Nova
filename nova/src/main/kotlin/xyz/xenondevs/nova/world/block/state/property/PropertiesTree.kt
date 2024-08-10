@file:Suppress("UNCHECKED_CAST")

package xyz.xenondevs.nova.world.block.state.property

import xyz.xenondevs.commons.collections.getOrSet
import xyz.xenondevs.nova.context.Context
import xyz.xenondevs.nova.context.intention.DefaultContextIntentions.BlockPlace

internal class PropertiesTree<E>(
    private val scopedProperties: List<ScopedBlockStateProperty<*>>,
    elementGenerator: (IntArray, Map<ScopedBlockStateProperty<*>, Any>) -> E
) {
    
    private val propertiesCount = scopedProperties.size
    
    val elements: List<E>
    private val root: Array<Any>
    
    init {
        if (scopedProperties.mapTo(HashSet()) { it.property }.size != scopedProperties.size)
            throw IllegalArgumentException("Duplicate properties")
        
        val propertiesCount = propertiesCount
        
        val elements = ArrayList<E>()
        // the number of children each node has
        val sizes = IntArray(propertiesCount) { scopedProperties[it].values.size }
        // the root node of the tree
        val root: Array<Any?> = arrayOfNulls(sizes[0])
        
        val path = IntArray(propertiesCount)
        
        fun nextPath(): Boolean {
            for (nodeIdx in path.lastIndex downTo 0) {
                val value = path[nodeIdx]
                if (value < sizes[nodeIdx] - 1) {
                    path[nodeIdx] = value + 1
                    return true
                } else {
                    path[nodeIdx] = 0
                }
            }
            
            return false
        }
        
        do {
            var currentArray: Array<Any?> = root
            for (nodeIdx in 0..<path.lastIndex) {
                currentArray = currentArray.getOrSet(path[nodeIdx]) { arrayOfNulls<Any>(sizes[nodeIdx + 1]) } as Array<Any?>
            }
            
            val propertyValues = HashMap<ScopedBlockStateProperty<*>, Any>()
            for ((nodeIdx, id) in path.withIndex()) {
                val scopedProperty = scopedProperties[nodeIdx]
                propertyValues[scopedProperty] = scopedProperty.idToValue(id) as Any
            }
            
            val element = elementGenerator(path.copyOf(), propertyValues)
            currentArray[path.last()] = element
            elements += element
        } while (nextPath())
        
        this.root = root as Array<Any>
        this.elements = elements
    }
    
    fun get(path: IntArray): E {
        require(path.size == propertiesCount) { "Path size must be equal to the number of properties" }
        
        var currentNode = root
        for (i in 0..<propertiesCount) {
            if (i == propertiesCount - 1) {
                return currentNode[path[i]] as E
            } else {
                currentNode = currentNode[path[i]] as Array<Any>
            }
        }
        
        throw IllegalStateException() // shouldn't happen
    }
    
    fun get(propertyValues: Map<BlockStateProperty<*>, Any>): E {
        return get(pathOf(propertyValues))
    }
    
    fun get(base: IntArray, depth: Int, newValueId: Int): E {
        require(base.size == propertiesCount) { "Path size must be equal to the number of properties" }
        
        var currentNode = root
        for (i in 0..<propertiesCount) {
            val valueId = if (i == depth) newValueId else base[i]
            if (i == propertiesCount - 1) {
                return currentNode[valueId] as E
            } else {
                currentNode = currentNode[valueId] as Array<Any>
            }
        }
        
        throw IllegalStateException() // shouldn't happen
    }
    
    fun get(ctx: Context<BlockPlace>): E {
        val arr = IntArray(propertiesCount)
        for (i in 0..<propertiesCount) {
            val property = scopedProperties[i] as ScopedBlockStateProperty<Any>
            arr[i] = property.valueToId(property.initializer(ctx))
        }
        
        return get(arr)
    }
    
    private fun pathOf(propertyValues: Map<BlockStateProperty<*>, Any>): IntArray {
        val path = IntArray(propertiesCount)
        for ((property, value) in propertyValues) {
            val (nodeIdx, scopedProperty) = find(property)
            path[nodeIdx] = (scopedProperty as ScopedBlockStateProperty<Any>).valueToId(value)
        }
        
        return path
    }
    
    fun <T : Any> find(property: BlockStateProperty<T>): IndexedValue<ScopedBlockStateProperty<T>> {
        val pair = scopedProperties.withIndex().firstOrNull { it.value == property || it.value.property == property }
            ?: throw IllegalArgumentException("Property $property is not part of this tree")
        return pair as IndexedValue<ScopedBlockStateProperty<T>>
    }
    
}