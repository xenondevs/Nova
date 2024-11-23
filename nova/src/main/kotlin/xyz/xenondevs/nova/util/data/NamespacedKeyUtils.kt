package xyz.xenondevs.nova.util.data

import net.kyori.adventure.key.Key
import org.bukkit.NamespacedKey
import org.bukkit.plugin.Plugin
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.addon.id

/**
 * Creates a new [NamespacedKey] using the [addon's][addon] [id][Addon.id] as the namespace and [key].
 */
fun NamespacedKey(addon: Addon, key: String): NamespacedKey =
    NamespacedKey(addon.id, key)

/**
 * Creates a new [Key] using the [plugin's][plugin] lowercase [name][Plugin.getName]
 * as the namespace and [value].
 */
fun Key(plugin: Plugin, value: String): Key =
    Key.key(plugin.name.lowercase(), value)

/**
 * Creates a new [Key] using the [addon's][addon] [id][Addon.id] as the namespace and [value].
 */
fun Key(addon: Addon, value: String): Key =
    Key.key(addon.id, value)