package xyz.xenondevs.nova.util.data

import net.kyori.adventure.key.Key
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.addon.id

fun NamespacedKey(addon: Addon, key: String): NamespacedKey =
    NamespacedKey(addon.id, key)

fun Key(addon: Addon, key: String): Key =
    Key.key(addon.id, key)