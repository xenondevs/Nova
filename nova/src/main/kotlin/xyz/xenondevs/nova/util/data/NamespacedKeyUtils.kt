package xyz.xenondevs.nova.util.data

import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.addon.id

fun NamespacedKey(addon: Addon, key: String): NamespacedKey =
    NamespacedKey(addon.id, key)