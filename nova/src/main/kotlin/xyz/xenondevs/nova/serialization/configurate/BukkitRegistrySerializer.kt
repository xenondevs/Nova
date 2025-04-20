package xyz.xenondevs.nova.serialization.configurate

import io.leangen.geantyref.TypeToken
import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.TypedKey
import io.papermc.paper.registry.set.RegistryKeySet
import io.papermc.paper.registry.set.RegistrySet
import io.papermc.paper.registry.tag.TagKey
import net.kyori.adventure.key.Key
import org.bukkit.Keyed
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.ScalarSerializer
import org.spongepowered.configurate.serialize.TypeSerializer
import xyz.xenondevs.nova.util.data.geantyrefTypeTokenOf
import xyz.xenondevs.nova.util.data.getList
import xyz.xenondevs.nova.util.data.setList
import java.lang.reflect.Type
import java.util.function.Predicate

internal inline fun <reified T : Keyed> BukkitRegistryEntrySerializer(key: RegistryKey<T>) =
    BukkitRegistryEntrySerializer(geantyrefTypeTokenOf<T>(), key)

internal class BukkitRegistryEntrySerializer<T : Keyed>(
    token: TypeToken<T>,
    private val registryKey: RegistryKey<T>
) : ScalarSerializer<T>(token) {
    
    private val registry by lazy { RegistryAccess.registryAccess().getRegistry(registryKey) }
    
    override fun deserialize(type: Type?, obj: Any?): T {
        return registry.getOrThrow(Key.key(obj.toString()))
    }
    
    override fun serialize(item: T, typeSupported: Predicate<Class<*>>?): Any {
        return item.key().toString()
    }
    
}

internal inline fun <reified T : Keyed> TagKeySerializer(registryKey: RegistryKey<T>) =
    TagKeySerializer(geantyrefTypeTokenOf<TagKey<T>>(), registryKey)

internal class TagKeySerializer<T : Keyed>(
    token: TypeToken<TagKey<T>>,
    private val registryKey: RegistryKey<T>
) : ScalarSerializer<TagKey<T>>(token) {
    
    override fun deserialize(type: Type, obj: Any): TagKey<T> {
        return TagKey.create(registryKey, Key.key(obj.toString().removePrefix("#")))
    }
    
    override fun serialize(item: TagKey<T>, typeSupported: Predicate<Class<*>>): Any {
        return item.key().toString()
    }
    
}

internal class RegistryKeySetSerializer<T : Keyed>(
    private val registryKey: RegistryKey<T>
) : TypeSerializer<RegistryKeySet<T>> {
    
    override fun deserialize(type: Type, node: ConfigurationNode): RegistryKeySet<T> {
        val scalar = node.rawScalar()
        if (scalar is String && scalar.startsWith('#')) {
            return RegistryAccess.registryAccess()
                .getRegistry(registryKey)
                .getTag(TagKey.create(registryKey, scalar.substring(1)))
        } else {
            val entries = node.getList<String>() ?: emptyList()
            return RegistrySet.keySet(registryKey, entries.map { TypedKey.create(registryKey, it) })
        }
    }
    
    override fun serialize(type: Type, obj: RegistryKeySet<T>?, node: ConfigurationNode) {
        if (obj == null) {
            node.raw(null)
            return
        }
        
        val entries = obj.values().map { it.key().toString() }
        node.setList(entries)
    }
    
}