package xyz.xenondevs.nova.util.data

import org.spongepowered.configurate.ConfigurationNode
import java.util.*

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