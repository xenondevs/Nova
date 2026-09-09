package xyz.xenondevs.nova.util

import net.kyori.adventure.key.Key
import net.kyori.adventure.key.Namespaced
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.addon.Addon

internal fun parseKey(string: String, defaultNamespace: Namespaced): Key {
    val separatorIndex = string.indexOf(Key.DEFAULT_SEPARATOR)
    val namespace = if (separatorIndex >= 1) string.substring(0, separatorIndex) else defaultNamespace.namespace()
    val value = if (separatorIndex >= 0) string.substring(separatorIndex + 1) else string
    return Key.key(namespace, value)
}

/**
 * Creates a new [NamespacedKey] using the [addon's][addon] [namespace][Addon.namespace] as the namespace and [key].
 */
fun NamespacedKey(addon: Addon, key: String): NamespacedKey =
    NamespacedKey(addon.namespace(), key)

internal fun novaKey(value: String) = NamespacedKey("nova", value)

/**
 * Creates a new [Key] using the [addon's][addon] [namespace][Addon.namespace] as the namespace and [value].
 */
@Deprecated("Can use built-in Key factory function", ReplaceWith("Key.key(addon, value)", "net.kyori.adventure.key.Key"))
fun Key(addon: Addon, value: String): Key =
    Key.key(addon.namespace(), value)

/**
 * Converts this [Key] to a [NamespacedKey].
 */
fun Key.toNamespacedKey(): NamespacedKey =
    NamespacedKey(this.namespace(), this.value())

/**
 * Converts this [Key] to a string by concatenating the [Key.namespace] and [Key.value] with [separator].
 */
fun Key.toString(separator: String): String =
    "${namespace()}$separator${value()}"