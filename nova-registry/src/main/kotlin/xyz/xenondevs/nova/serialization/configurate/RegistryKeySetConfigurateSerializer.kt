package xyz.xenondevs.nova.serialization.configurate

import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.TypedKey
import io.papermc.paper.registry.set.RegistryKeySet
import io.papermc.paper.registry.set.RegistrySet
import io.papermc.paper.registry.tag.Tag
import io.papermc.paper.registry.tag.TagKey
import net.kyori.adventure.key.Key
import org.bukkit.Keyed
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.TypeSerializer
import java.lang.reflect.Type

/**
 * A configurate serializer for [RegistryKeySet].
 *
 * Serializes a tag to `#namespace:value` and a direct key set
 * to a list of `namespace:value` strings (or just a single string if there's only one entry).
 */
class RegistryKeySetConfigurateSerializer<T : Keyed>(
    private val registryKey: RegistryKey<T>,
    registryAccess: RegistryAccess = RegistryAccess.registryAccess()
) : TypeSerializer<RegistryKeySet<T>> {
    
    private val registry by lazy { registryAccess.getRegistry(registryKey) }
    
    override fun deserialize(type: Type, node: ConfigurationNode): RegistryKeySet<T> {
        val scalar = node.rawScalar()
        if (scalar is String && scalar.startsWith("#")) {
            val tagKey = Key.key(scalar.substring(1))
            return registry.getTag(TagKey.create(registryKey, tagKey))
        } else {
            val entries = node.getList(String::class.java) ?: emptyList()
            return RegistrySet.keySet(registryKey, entries.map { TypedKey.create(registryKey, Key.key(it)) })
        }
    }
    
    override fun serialize(type: Type, obj: RegistryKeySet<T>?, node: ConfigurationNode) {
        if (obj == null) {
            node.raw(null)
            return
        }
        
        if (obj is Tag<T>) {
            node.raw("#${obj.tagKey().key().asString()}")
        } else {
            val entries = obj.values().map { it.key().asString() }
            if (entries.size == 1) {
                node.raw(entries[0])
            } else {
                node.setList(String::class.java, entries)
            }
        }
    }
    
}


