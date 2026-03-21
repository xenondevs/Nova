package xyz.xenondevs.nova.serialization.configurate

import io.leangen.geantyref.TypeToken
import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import net.kyori.adventure.key.Key
import org.bukkit.Keyed
import org.bukkit.Registry
import org.spongepowered.configurate.serialize.ScalarSerializer
import xyz.xenondevs.nova.registry.NovaRegistry
import xyz.xenondevs.nova.registry.NovaRegistryElement
import java.lang.reflect.Type
import java.util.function.Predicate

/**
 * Creates a [NovaRegistryElementConfigurateSerializer] for the given [registry].
 */
inline fun <reified T : NovaRegistryElement<T>> NovaRegistryElementConfigurateSerializer(
    registry: NovaRegistry<T>
): NovaRegistryElementConfigurateSerializer<T> =
    NovaRegistryElementConfigurateSerializer(registry, geantyrefTypeTokenOf())

/**
 * A configurate serializer for [NovaRegistryElement] that serializes the element by its key.
 */
class NovaRegistryElementConfigurateSerializer<T : NovaRegistryElement<T>>(
    private val registry: NovaRegistry<T>,
    type: TypeToken<T>
) : ScalarSerializer<T>(type) {
    
    override fun deserialize(type: Type, obj: Any): T {
        val key = Key.key(obj as String)
        return registry.getValueOrThrow(key)
    }
    
    override fun serialize(item: T, typeSupported: Predicate<Class<*>>): Any {
        return item.key.asString()
    }
    
}

/**
 * Creates a [PaperRegistryElementConfigurateSerializer] for the given [registryKey].
 */
inline fun <reified T : Keyed> PaperRegistryElementConfigurateSerializer(
    registryKey: RegistryKey<T>,
    registryAccess: RegistryAccess = RegistryAccess.registryAccess()
): PaperRegistryElementConfigurateSerializer<T> =
    PaperRegistryElementConfigurateSerializer(registryKey, geantyrefTypeTokenOf(), registryAccess)

/**
 * A configurate serializer for Paper registry elements that serializes the element by its key.
 */
class PaperRegistryElementConfigurateSerializer<T : Keyed>(
    registryKey: RegistryKey<T>,
    type: TypeToken<T>,
    registryAccess: RegistryAccess = RegistryAccess.registryAccess()
) : ScalarSerializer<T>(type) {
    
    private val registry: Registry<T> by lazy { registryAccess.getRegistry(registryKey) }
    
    override fun deserialize(type: Type, obj: Any): T {
        val key = Key.key(obj as String)
        return registry.getOrThrow(key)
    }
    
    override fun serialize(item: T, typeSupported: Predicate<Class<*>>): Any {
        return item.key().toString()
    }
    
}
