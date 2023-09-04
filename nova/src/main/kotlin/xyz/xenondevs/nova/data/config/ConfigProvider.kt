@file:Suppress("UNCHECKED_CAST")

package xyz.xenondevs.nova.data.config

import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.get
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.immutable.map
import java.lang.reflect.Type
import java.nio.file.Path
import kotlin.reflect.KType
import kotlin.reflect.jvm.javaType
import kotlin.reflect.typeOf

inline fun <reified T : Any> ConfigProvider.entry(vararg path: String): Provider<T> =
    entry(typeOf<T>().javaType, *path)

fun <T : Any> ConfigProvider.entry(type: KType, vararg path: String): Provider<T> =
    entry(type.javaType, *path)

fun <T : Any> ConfigProvider.entry(type: Type, vararg path: String): Provider<T> =
    map {
        val node = it.node(*path)
        if (node.virtual())
            throw NoSuchElementException("Missing config entry '${path.joinToString(" > ")}' in $relPath")
        
        try {
            return@map node.get(type)!! as T
        } catch (t: Throwable) {
            throw IllegalStateException("Config entry '${node.path().joinToString(" > ")}' in $relPath could not be deserialized to $type", t)
        }
    }

inline fun <reified T : Any> ConfigProvider.entry(vararg paths: Array<String>): Provider<T> =
    entry(typeOf<T>().javaType, *paths)

fun <T : Any> ConfigProvider.entry(type: KType, vararg paths: Array<String>): Provider<T> =
    entry(type.javaType, *paths)

fun <T : Any> ConfigProvider.entry(type: Type, vararg paths: Array<String>): Provider<T> =
    map {
        var node: ConfigurationNode? = null
        for (path in paths) {
            val possibleNode = it.node(*path)
            if (!possibleNode.virtual()) {
                node = possibleNode
                break
            }
        }
        
        if (node == null || node.virtual())
            throw NoSuchElementException("Missing config entry ${paths.joinToString(" or ") { path -> "'${path.joinToString(" > ")}'" }} in $relPath")
        
        try {
            return@map node.get(type)!! as T
        } catch (t: Throwable) {
            throw IllegalStateException("Config entry '${node.path().joinToString(" > ")}' in $relPath could not be deserialized to $type", t)
        }
    }

inline fun <reified T : Any> ConfigProvider.optionalEntry(vararg path: String): Provider<T?> =
    optionalEntry(typeOf<T>().javaType, *path)

fun <T : Any> ConfigProvider.optionalEntry(type: KType, vararg path: String): Provider<T?> =
    optionalEntry(type.javaType, *path)

fun <T : Any> ConfigProvider.optionalEntry(type: Type, vararg path: String): Provider<T?> =
    map { it.node(*path).get(type) as? T }

inline fun <reified T : Any> ConfigProvider.optionalEntry(vararg paths: Array<String>): Provider<T?> =
    map {
        var node: ConfigurationNode? = null
        for (path in paths) {
            val possibleNode = it.node(*path)
            if (possibleNode != null) {
                node = possibleNode
                break
            }
        }
        
        node?.get<T?>()
    }

class ConfigProvider internal constructor(
    private val path: Path,
    val relPath: String,
    private val loadValidation: () -> Boolean
) : Provider<CommentedConfigurationNode>() {
    
    override fun loadValue(): CommentedConfigurationNode {
        if (!loadValidation())
            throw IllegalStateException("The config at '$path' is not loaded yet")
        
        return Configs.createLoader(path).load()
    }
    
}