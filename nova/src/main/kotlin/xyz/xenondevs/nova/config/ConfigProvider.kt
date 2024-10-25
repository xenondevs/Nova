@file:Suppress("UNCHECKED_CAST")

package xyz.xenondevs.nova.config

import net.minecraft.resources.ResourceLocation
import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.ScopedConfigurationNode
import xyz.xenondevs.commons.provider.AbstractProvider
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.UnstableProviderApi
import xyz.xenondevs.commons.provider.combinedProvider
import xyz.xenondevs.commons.provider.map
import xyz.xenondevs.commons.provider.weakCombinedProvider
import xyz.xenondevs.commons.provider.weakMap
import java.lang.ref.WeakReference
import java.lang.reflect.Type
import java.nio.file.Path
import java.util.concurrent.locks.ReentrantLock
import kotlin.reflect.KType
import kotlin.reflect.jvm.javaType
import kotlin.reflect.typeOf

/**
 * Gets or creates a [Provider] for a child node under [path].
 */
@JvmName("nodeTyped")
fun <C : ScopedConfigurationNode<C>> Provider<C>.node(vararg path: String): Provider<C> =
    map { node -> node.node(*path) }

/**
 * Gets or creates a [Provider] for a child node under [path].
 *
 * The returned provider will only be stored in a [WeakReference] in the parent provider.
 */
@JvmName("weakNodeTyped")
fun <C : ScopedConfigurationNode<C>> Provider<C>.weakNode(vararg path: String): Provider<C> =
    weakMap { node -> node.node(*path) }

/**
 * Gets or creates a [Provider] for a child node under [path].
 */
fun Provider<ConfigurationNode>.node(vararg path: String): Provider<ConfigurationNode> =
    map { node -> node.node(*path) }

/**
 * Gets or creates a [Provider] for a child node under [path].
 *
 * The returned provider will only be stored in a [WeakReference] in the parent provider.
 */
fun Provider<ConfigurationNode>.weakNode(vararg path: String): Provider<ConfigurationNode> =
    weakMap { node -> node.node(*path) }

/**
 * Gets an entry [Provider] for a value of type [T] under [path].
 *
 * @throws NoSuchElementException if the entry does not exist
 * @throws IllegalStateException if the entry could not be deserialized to [T]
 */
inline fun <reified T : Any> Provider<ConfigurationNode>.entry(vararg path: String): Provider<T> =
    entry(typeOf<T>().javaType, *path)

/**
 * Gets an entry [Provider] for a value of type [T] under [path].
 *
 * The returned provider will only be stored in a [WeakReference] in the parent provider.
 *
 * @throws NoSuchElementException if the entry does not exist
 * @throws IllegalStateException if the entry could not be deserialized to [T]
 */
inline fun <reified T : Any> Provider<ConfigurationNode>.weakEntry(vararg path: String): Provider<T> =
    weakEntry(typeOf<T>().javaType, *path)

/**
 * Gets an entry [Provider] for a value of [type] under [path].
 *
 * @throws NoSuchElementException if the entry does not exist
 * @throws IllegalStateException if the entry could not be deserialized to [type]
 */
fun <T : Any> Provider<ConfigurationNode>.entry(type: KType, vararg path: String): Provider<T> =
    entry(type.javaType, *path)

/**
 * Gets an entry [Provider] for a value of [type] under [path].
 *
 * The returned provider will only be stored in a [WeakReference] in the parent provider.
 *
 * @throws NoSuchElementException if the entry does not exist
 * @throws IllegalStateException if the entry could not be deserialized to [type]
 */
fun <T : Any> Provider<ConfigurationNode>.weakEntry(type: KType, vararg path: String): Provider<T> =
    weakEntry(type.javaType, *path)

/**
 * Gets an entry [Provider] for a value of [type] under [path].
 *
 * @throws NoSuchElementException if the entry does not exist
 * @throws IllegalStateException if the entry could not be deserialized to [type]
 */
fun <T : Any> Provider<ConfigurationNode>.entry(type: Type, vararg path: String): Provider<T> =
    node(*path).map { getEntry(it, type, *path) }

/**
 * Gets an entry [Provider] for a value of [type] under [path].
 *
 * The returned provider will only be stored in a [WeakReference] in the parent provider.
 *
 * @throws NoSuchElementException if the entry does not exist
 * @throws IllegalStateException if the entry could not be deserialized to [type]
 */
fun <T : Any> Provider<ConfigurationNode>.weakEntry(type: Type, vararg path: String): Provider<T> =
    weakNode(*path).weakMap { getEntry(it, type, *path) }

private fun <T : Any> Provider<ConfigurationNode>.getEntry(node: ConfigurationNode, type: Type, vararg path: String): T {
    if (node.virtual())
        throw NoSuchElementException("Missing config entry '${path.joinToString(" > ")}' in '${fullPath()}'")
    
    try {
        return node.get(type)!! as T
    } catch (t: Throwable) {
        throw IllegalStateException("Config entry '${fullPath()} > ${path.joinToString(" > ")}' could not be deserialized to $type", t)
    }
}

/**
 * Gets an entry [Provider] for a value of type [T] under the first existing path from [paths].
 *
 * @throws NoSuchElementException if no entry exists
 * @throws IllegalStateException if the entry could not be deserialized to [T]
 */
inline fun <reified T : Any> Provider<ConfigurationNode>.entry(vararg paths: Array<String>): Provider<T> =
    entry(typeOf<T>().javaType, *paths)

/**
 * Gets an entry [Provider] for a value of type [T] under the first existing path from [paths].
 *
 * The returned provider will only be stored in a [WeakReference] in the parent provider.
 *
 * @throws NoSuchElementException if no entry exists
 * @throws IllegalStateException if the entry could not be deserialized to [T]
 */
inline fun <reified T : Any> Provider<ConfigurationNode>.weakEntry(vararg paths: Array<String>): Provider<T> =
    weakEntry(typeOf<T>().javaType, *paths)

/**
 * Gets an entry [Provider] for a value of [type] under the first existing path from [paths].
 *
 * @throws NoSuchElementException if no entry exists
 * @throws IllegalStateException if the entry could not be deserialized to [type]
 */
fun <T : Any> Provider<ConfigurationNode>.entry(type: KType, vararg paths: Array<String>): Provider<T> =
    entry(type.javaType, *paths)

/**
 * Gets an entry [Provider] for a value of [type] under the first existing path from [paths].
 *
 * The returned provider will only be stored in a [WeakReference] in the parent provider.
 *
 * @throws NoSuchElementException if no entry exists
 * @throws IllegalStateException if the entry could not be deserialized to [type]
 */
fun <T : Any> Provider<ConfigurationNode>.weakEntry(type: KType, vararg paths: Array<String>): Provider<T> =
    weakEntry(type.javaType, *paths)

/**
 * Gets an entry [Provider] for a value of [type] under the first existing path from [paths].
 *
 * @throws NoSuchElementException if no entry exists
 * @throws IllegalStateException if the entry could not be deserialized to [type]
 */
fun <T : Any> Provider<ConfigurationNode>.entry(type: Type, vararg paths: Array<String>): Provider<T> =
    combinedProvider(paths.map { node(*it) }).map { getEntry(it, type, *paths) }

/**
 * Gets an entry [Provider] for a value of [type] under the first existing path from [paths].
 *
 * The returned provider will only be stored in a [WeakReference] in the parent provider.
 *
 * @throws NoSuchElementException if no entry exists
 * @throws IllegalStateException if the entry could not be deserialized to [type]
 */
fun <T : Any> Provider<ConfigurationNode>.weakEntry(type: Type, vararg paths: Array<String>): Provider<T> =
    weakCombinedProvider(paths.map { weakNode(*it) }).weakMap { getEntry(it, type, *paths) }

private fun <T: Any> Provider<ConfigurationNode>.getEntry(nodes: List<ConfigurationNode>, type: Type, vararg paths: Array<String>): T {
    var node: ConfigurationNode? = null
    for (possibleNode in nodes) {
        if (!possibleNode.virtual()) {
            node = possibleNode
            break
        }
    }
    
    if (node == null || node.virtual())
        throw NoSuchElementException("Missing config entry ${paths.joinToString(" or ") { path -> "'${path.joinToString(" > ")}'" }} in ${fullPath()}")
    
    try {
        return node.get(type)!! as T
    } catch (t: Throwable) {
        throw IllegalStateException("Config entry '${fullPath()} > ${node.path().joinToString(" > ")}' could not be deserialized to $type", t)
    }
}

/**
 * Gets an optional entry [Provider] for a value of type [T] under [path], whose value
 * will be null if the entry does not exist or could not be deserialized to [T].
 */
inline fun <reified T : Any> Provider<ConfigurationNode>.optionalEntry(vararg path: String): Provider<T?> =
    optionalEntry(typeOf<T>().javaType, *path)

/**
 * Gets an optional entry [Provider] for a value of type [T] under [path], whose value
 * will be null if the entry does not exist or could not be deserialized to [T].
 *
 * The returned provider will only be stored in a [WeakReference] in the parent provider.
 */
inline fun <reified T : Any> Provider<ConfigurationNode>.weakOptionalEntry(vararg path: String): Provider<T?> =
    weakOptionalEntry(typeOf<T>().javaType, *path)

/**
 * Gets an optional entry [Provider] for a value of [type] under [path], whose value
 * will be null if the entry does not exist or could not be deserialized to [type].
 */
fun <T : Any> Provider<ConfigurationNode>.optionalEntry(type: KType, vararg path: String): Provider<T?> =
    optionalEntry(type.javaType, *path)

/**
 * Gets an optional entry [Provider] for a value of [type] under [path], whose value
 * will be null if the entry does not exist or could not be deserialized to [type].
 *
 * The returned provider will only be stored in a [WeakReference] in the parent provider.
 */
fun <T : Any> Provider<ConfigurationNode>.weakOptionalEntry(type: KType, vararg path: String): Provider<T?> =
    weakOptionalEntry(type.javaType, *path)

/**
 * Gets an optional entry [Provider] for a value of [type] under [path], whose value
 * will be null if the entry does not exist or could not be deserialized to [type].
 */
fun <T : Any> Provider<ConfigurationNode>.optionalEntry(type: Type, vararg path: String): Provider<T?> =
    node(*path).map { if (!it.virtual()) it.get(type) as? T else null }

/**
 * Gets an optional entry [Provider] for a value of [type] under [path], whose value
 * will be null if the entry does not exist or could not be deserialized to [type].
 *
 * The returned provider will only be stored in a [WeakReference] in the parent provider.
 */
fun <T : Any> Provider<ConfigurationNode>.weakOptionalEntry(type: Type, vararg path: String): Provider<T?> =
    weakNode(*path).weakMap { if (!it.virtual()) it.get(type) as? T else null }

/**
 * Gets an optional entry [Provider] for a value of type [T] under the first existing path from [paths],
 * whose value will be null if no entry exists or could not be deserialized to [T].
 */
inline fun <reified T : Any> Provider<ConfigurationNode>.optionalEntry(vararg paths: Array<String>): Provider<T?> =
    optionalEntry(typeOf<T>().javaType, *paths)

/**
 * Gets an optional entry [Provider] for a value of type [T] under the first existing path from [paths],
 * whose value will be null if no entry exists or could not be deserialized to [T].
 *
 * The returned provider will only be stored in a [WeakReference] in the parent provider.
 */
inline fun <reified T : Any> Provider<ConfigurationNode>.weakOptionalEntry(vararg paths: Array<String>): Provider<T?> =
    weakOptionalEntry(typeOf<T>().javaType, *paths)

/**
 * Gets an optional entry [Provider] for a value of [type] under the first existing path from [paths],
 * whose value will be null if no entry exists or could not be deserialized to [type].
 */
fun <T : Any> Provider<ConfigurationNode>.optionalEntry(type: KType, vararg paths: Array<String>): Provider<T?> =
    optionalEntry(type.javaType, *paths)

/**
 * Gets an optional entry [Provider] for a value of [type] under the first existing path from [paths],
 * whose value will be null if no entry exists or could not be deserialized to [type].
 *
 * The returned provider will only be stored in a [WeakReference] in the parent provider.
 */
fun <T : Any> Provider<ConfigurationNode>.weakOptionalEntry(type: KType, vararg paths: Array<String>): Provider<T?> =
    weakOptionalEntry(type.javaType, *paths)

/**
 * Gets an optional entry [Provider] for a value of [type] under the first existing path from [paths],
 * whose value will be null if no entry exists or could not be deserialized to [type].
 */
fun <T : Any> Provider<ConfigurationNode>.optionalEntry(type: Type, vararg paths: Array<String>): Provider<T?> =
    combinedProvider(paths.map { node(*it) }).map { getOptionalEntry(it, type) }

/**
 * Gets an optional entry [Provider] for a value of [type] under the first existing path from [paths],
 * whose value will be null if no entry exists or could not be deserialized to [type].
 *
 * The returned provider will only be stored in a [WeakReference] in the parent provider.
 */
fun <T : Any> Provider<ConfigurationNode>.weakOptionalEntry(type: Type, vararg paths: Array<String>): Provider<T?> =
    weakCombinedProvider(paths.map { weakNode(*it) }).weakMap { getOptionalEntry(it, type) }

private fun <T : Any> Provider<ConfigurationNode>.getOptionalEntry(nodes: List<ConfigurationNode>, type: Type): T? {
    var node: ConfigurationNode? = null
    for (possibleNode in nodes) {
        if (!possibleNode.virtual()) {
            node = possibleNode
            break
        }
    }
    
    return node?.get(type) as? T
}

private fun Provider<ConfigurationNode>.fullPath(): String {
    val filePath = findFilePath()
    val path = get().path()
    
    val builder = StringBuilder()
    if (path.size() > 0) {
        if (filePath != null) {
            builder.append(filePath)
            builder.append(" > ")
        }
        builder.append(path.joinToString(" > "))
    } else if (filePath != null) {
        builder.append(filePath)
    }
    
    return builder.toString()
}

@OptIn(UnstableProviderApi::class)
private fun Provider<ConfigurationNode>.findFilePath(): String? {
    this as AbstractProvider<ConfigurationNode>
    
    val queue = ArrayDeque<AbstractProvider<*>>()
    queue.add(this)
    
    while (queue.isNotEmpty()) {
        val current = queue.removeFirst()
        if (current is RootConfigProvider) {
            return current.configId.toString()
        }
        queue.addAll(current.parents)
    }
    
    return null
}

@OptIn(UnstableProviderApi::class)
internal class RootConfigProvider internal constructor(
    val path: Path,
    val configId: ResourceLocation
) : AbstractProvider<CommentedConfigurationNode>(ReentrantLock()) {
    
    @Volatile
    var loaded = false
        private set
    
    init {
        subscribe { loaded = true }
    }
    
    override fun pull(): CommentedConfigurationNode {
        // empty placeholder that is replaced by the actual node when the config is loaded
        return Configs.createBuilder().build().createNode()
    }
    
}