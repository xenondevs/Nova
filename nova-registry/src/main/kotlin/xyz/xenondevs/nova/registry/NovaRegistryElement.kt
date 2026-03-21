package xyz.xenondevs.nova.registry

import net.kyori.adventure.key.Key
import org.bukkit.Keyed
import org.bukkit.NamespacedKey

/**
 * Something that is registered in a [NovaRegistry].
 */
interface NovaRegistryElement<out S : NovaRegistryElement<S>> : Keyed {
    
    /**
     * The [RegistryEntry] of this element.
     */
    val entry: RegistryEntry.Nova<S>
    
    /**
     * The [Key] of this element.
     * Equivalent to `entry.key`.
     */
    val key: Key
        get() = entry.key
    
    @Deprecated("Renamed to key", ReplaceWith("key"))
    val id: Key
        get() = entry.key
    
    override fun key(): Key = key
    override fun getKey(): NamespacedKey = NamespacedKey(key.namespace(), key.value())
    
}