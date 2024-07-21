package xyz.xenondevs.nova.util.data

import io.leangen.geantyref.TypeToken
import org.snakeyaml.engine.v2.common.FlowStyle
import org.snakeyaml.engine.v2.common.ScalarStyle
import org.snakeyaml.engine.v2.nodes.MappingNode
import org.snakeyaml.engine.v2.nodes.Node
import org.snakeyaml.engine.v2.nodes.NodeTuple
import org.snakeyaml.engine.v2.nodes.ScalarNode
import org.snakeyaml.engine.v2.nodes.SequenceNode
import org.snakeyaml.engine.v2.nodes.Tag
import org.spongepowered.configurate.ConfigurationNode
import java.util.*
import kotlin.collections.ArrayDeque
import kotlin.reflect.javaType
import kotlin.reflect.jvm.javaType
import kotlin.reflect.typeOf

@Suppress("UNCHECKED_CAST")
fun <T : ConfigurationNode> T.walk(): Sequence<T> = sequence {
    val unexplored = LinkedList<T>()
    unexplored += this@walk
    while (unexplored.isNotEmpty()) {
        val node = unexplored.poll()
        for ((_, child) in node.childrenMap()) {
            unexplored += child as T
        }
        yield(node)
    }
}

inline fun <reified T> ConfigurationNode.get(): T? {
    return get(typeOf<T>().javaType) as? T
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T> ConfigurationNode.getList(): List<T>? {
    return getList(TypeToken.get(typeOf<T>().javaType)) as List<T>?
}

/**
 * Performs a depth-first traversal of all mapping nodes in the tree and calls the [consume] function for each node.
 * Does not walk the root node.
 */
internal fun Node.walk(consume: (path: List<String>, keyNode: ScalarNode, valueNode: Node) -> NodeWalkDecision) {
    if (this !is MappingNode)
        return
    
    val stack = ArrayDeque<Pair<List<String>, Pair<ScalarNode, Node>>>()
    for (tuple in value.asReversed()) {
        val key = tuple.keyNode as ScalarNode
        stack.addFirst(listOf(key.value) to (key to tuple.valueNode))
    }
    
    while (stack.isNotEmpty()) {
        val (path, keyValueNodes) = stack.removeFirst()
        val (keyNode, valueNode) = keyValueNodes
        val decision = consume(path, keyNode, valueNode)
        
        when (decision) {
            NodeWalkDecision.CONTINUE -> {
                if (valueNode is MappingNode) {
                    for ((subKeyNode, subValueNode) in valueNode.value.asReversed()) {
                        subKeyNode as ScalarNode
                        stack.addFirst(path + subKeyNode.value to (subKeyNode to subValueNode))
                    }
                }
            }
            NodeWalkDecision.SKIP -> Unit
            NodeWalkDecision.STOP -> break
        }
    }
}

/**
 * Specifies a decision to be made during [Node] tree walk.
 */
internal enum class NodeWalkDecision {
    
    /**
     * Continues to the next node normally
     */
    CONTINUE,
    
    /**
     * Skips the children of the current node
     */
    SKIP,
    
    /**
     * Stops the walk
     */
    STOP
    
}

/**
 * Gets the ([ScalarNode], [Node]) tuple at the specified path or null if it doesn't exist.
 */
internal fun Node.get(path: List<String>): Pair<ScalarNode, Node>? {
    require(path.isNotEmpty())
    
    var node: Pair<ScalarNode?, Node>? = null to this
    for (key in path) {
        val map = node?.second
        if (map !is MappingNode)
            return null
        
        val tuple = map.value
            .firstOrNull { (it.keyNode as ScalarNode).value == key }
            ?: return null
        node = tuple.keyNode as ScalarNode to tuple.valueNode
    }
    
    return node?.let { (first, second) -> Pair(first!!, second) }
}

/**
 * Removes the node at the specified [path] in the tree.
 */
internal fun Node.remove(path: List<String>) {
    require(path.isNotEmpty())
    if (this !is MappingNode)
        return
    
    val node: MappingNode
    if (path.size > 1) {
        node = get(path.subList(0, path.size - 1))?.second as? MappingNode ?: return
    } else {
        node = this
    }
    
    val key = path.last()
    node.value.removeIf { (it.keyNode as ScalarNode).value == key }
}

/**
 * Sets [value] at the specified [path] in the tree, potentially replacing existing nodes,
 * and tries to order it after [afterEntry] if it exists.
 */
internal fun Node.set(path: List<String>, value: Node, afterEntry: String = "") {
    require(this is MappingNode)
    
    var node: MappingNode = this
    for (i in path.indices) {
        val key = path[i]
        val tuple = node.value.firstOrNull { (it.keyNode as ScalarNode).value == key }
        val valueNode = tuple?.valueNode
        if (valueNode !is MappingNode) { // non-existent or different type that cannot be further traversed
            if (tuple != null)
                node.value.remove(tuple)
            val remPath = path.subList(i + 1, path.size)
            val newTuple = NodeTuple(ScalarNode(Tag.STR, key, ScalarStyle.PLAIN), createMapping(remPath, value))
            val pos = node.value.indexOfFirst { (it.keyNode as ScalarNode).value == afterEntry }
            node.value.add(pos + 1, newTuple)
        } else {
            node = valueNode
        }
    }
}

/**
 * Creates a node tree that is just a single line of [MappingNode] until [value].
 */
private fun createMapping(path: List<String>, value: Node): Node {
    var node: Node = value
    for (key in path.asReversed()) {
        node = MappingNode(
            Tag.MAP,
            mutableListOf(NodeTuple(ScalarNode(Tag.STR, key, ScalarStyle.PLAIN), node)),
            FlowStyle.AUTO
        )
    }
    
    return node
}

/**
 * Checks [this][Node] and [other] for deep equality.
 * Two nodes are considered equal if they are structurally equal and their leaf nodes have the same values.
 */
internal fun Node?.deepEquals(other: Node?): Boolean {
    when {
        this == null && other == null -> return true
        
        this is ScalarNode && other is ScalarNode -> return value == other.value && tag == other.tag
        
        this is MappingNode && other is MappingNode -> {
            val thisValues = value
            val otherValues = other.value
            if (thisValues.size != otherValues.size)
                return false
            
            for (i in thisValues.indices) {
                val thisTuple = thisValues[i]
                val otherTuple = otherValues[i]
                
                if (!thisTuple.keyNode.deepEquals(otherTuple.keyNode))
                    return false
                if (!thisTuple.valueNode.deepEquals(otherTuple.valueNode))
                    return false
            }
            
            return true
        }
        
        this is SequenceNode && other is SequenceNode -> {
            val thisValues = value
            val otherValues = other.value
            if (thisValues.size != otherValues.size)
                return false
            
            for (i in thisValues.indices) {
                if (!thisValues[i].deepEquals(otherValues[i]))
                    return false
            }
            
            return true
        }
        
        else -> return false
    }
}

internal operator fun NodeTuple.component1(): Node = keyNode
internal operator fun NodeTuple.component2(): Node = valueNode
