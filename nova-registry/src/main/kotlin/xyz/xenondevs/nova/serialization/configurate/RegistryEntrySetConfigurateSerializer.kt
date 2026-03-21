package xyz.xenondevs.nova.serialization.configurate

import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.TypedKey
import io.papermc.paper.registry.tag.TagKey
import net.kyori.adventure.key.Key
import org.bukkit.Keyed
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.TypeSerializer
import xyz.xenondevs.nova.registry.NovaRegistry
import xyz.xenondevs.nova.registry.NovaRegistryElement
import xyz.xenondevs.nova.registry.RegistryEntry
import xyz.xenondevs.nova.registry.RegistryEntrySet
import xyz.xenondevs.nova.registry.registryEntrySetOf
import java.lang.reflect.Type

/**
 * A configurate serializer for [RegistryEntrySet.Nova].
 * 
 * Serializes [RegistryEntrySet.Nova.Tag] to `#namespace:value` and [RegistryEntrySet.Nova.Direct]
 * to a list of `namespace:value` strings (or just a single string if there's only one entry).
 */
class NovaRegistryEntrySetConfigurateSerializer<T : NovaRegistryElement<T>>(
    private val registry: NovaRegistry<T>
) : TypeSerializer<RegistryEntrySet.Nova<T>> {
    
    override fun deserialize(type: Type, node: ConfigurationNode): RegistryEntrySet.Nova<T> {
        val scalar = node.rawScalar()
        if (scalar is String && scalar.startsWith("#")) {
            val tagKey = Key.key(scalar.substring(1))
            return registry.getTag(tagKey)
        } else {
            val entries = node.getList(String::class.java)!!
            return registryEntrySetOf(entries.map { registry[Key.key(it)] })
        }
    }
    
    override fun serialize(type: Type, obj: RegistryEntrySet.Nova<T>?, node: ConfigurationNode) {
        if (obj == null) {
            node.raw(null)
            return
        }
        
        when (obj) {
            is RegistryEntrySet.Nova.Tag<T> -> {
                node.raw("#${obj.tagKey.asString()}")
            }
            
            is RegistryEntrySet.Nova.Direct<T> -> {
                val entries = obj.entries.map { it.key.asString() }
                if (entries.size == 1) {
                    node.raw(entries[0])
                } else {
                    node.setList(String::class.java, entries)
                }
            }
        }
    }
    
}

/**
 * A configurate serializer for [RegistryEntrySet.Paper].
 * 
 * Serializes [RegistryEntrySet.Paper.Tag] to `#namespace:value` and [RegistryEntrySet.Paper.Direct]
 * to a list of `namespace:value` strings (or just a single string if there's only one entry).
 */
class PaperRegistryEntrySetConfigurateSerializer<T : Keyed>(
    private val registryKey: RegistryKey<T>,
    private val registryAccess: RegistryAccess = RegistryAccess.registryAccess()
) : TypeSerializer<RegistryEntrySet.Paper<T>> {
    
    override fun deserialize(type: Type, node: ConfigurationNode): RegistryEntrySet.Paper<T> {
        val scalar = node.rawScalar()
        if (scalar is String && scalar.startsWith("#")) {
            val tagKey = Key.key(scalar.substring(1))
            return registryEntrySetOf(TagKey.create(registryKey, tagKey), registryAccess)
        } else {
            val entries = node.getList(String::class.java)!!
            return registryEntrySetOf(entries.map { TypedKey.create(registryKey, Key.key(it)) }, registryAccess)
        }
    }
    
    override fun serialize(type: Type, obj: RegistryEntrySet.Paper<T>?, node: ConfigurationNode) {
        if (obj == null) {
            node.raw(null)
            return
        }
        
        when (obj) {
            is RegistryEntrySet.Paper.Tag<T> -> {
                node.raw("#${obj.tagKey.key().asString()}")
            }
            
            is RegistryEntrySet.Paper.Direct<T> -> {
                val entries = obj.entries.map { it.key.asString() }
                if (entries.size == 1) {
                    node.raw(entries[0])
                } else {
                    node.setList(String::class.java, entries)
                }
            }
        }
    }
    
}

/**
 * A configurate serializer for [RegistryEntrySet.Mixed].
 *
 * Serializes [RegistryEntrySet.Mixed.Tag] to `#namespace:value` and [RegistryEntrySet.Mixed.Direct]
 * to a list of `namespace:value` strings (or just a single string if there's only one entry).
 *
 * Nova registries will be prioritized for direct entries.
 * Tags will be merged from both registries.
 */
class MixedRegistryEntrySetConfigurateSerializer<N : NovaRegistryElement<N>, P : Keyed>(
    private val novaRegistry: NovaRegistry<N>,
    private val paperRegistry: RegistryKey<P>,
    private val registryAccess: RegistryAccess = RegistryAccess.registryAccess()
) : TypeSerializer<RegistryEntrySet.Mixed<N, P>> {
    
    override fun deserialize(type: Type, node: ConfigurationNode): RegistryEntrySet.Mixed<N, P> {
        val scalar = node.rawScalar()
        if (scalar is String && scalar.startsWith("#")) {
            val tagKey = Key.key(scalar.substring(1))
            return registryEntrySetOf(tagKey, novaRegistry, paperRegistry, registryAccess)
        } else {
            val elements = node.getList(String::class.java)!!
            val entries = elements.map { RegistryEntry.either(Key.key(it), novaRegistry, paperRegistry, registryAccess) }
            return registryEntrySetOf(entries, novaRegistry, paperRegistry)
        }
    }
    
    override fun serialize(type: Type, obj: RegistryEntrySet.Mixed<N, P>?, node: ConfigurationNode) {
        if (obj == null) {
            node.raw(null)
            return
        }
        
        when (obj) {
            is RegistryEntrySet.Mixed.Tag<N, P> -> {
                node.raw("#${obj.tagKey.asString()}")
            }
            
            is RegistryEntrySet.Mixed.Direct<N, P> -> {
                val entries = obj.entries.map { it.key.asString() }
                if (entries.size == 1) {
                    node.raw(entries[0])
                } else {
                    node.setList(String::class.java, entries)
                }
            }
        }
    }
    
}
