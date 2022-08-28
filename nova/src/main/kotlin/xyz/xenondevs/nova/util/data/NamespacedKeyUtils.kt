@file:Suppress("DEPRECATION")

package xyz.xenondevs.nova.util.data

import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.addon.Addon

fun NamespacedKey(addon: Addon, key: String): NamespacedKey =
    NamespacedKey(addon.description.id, key)