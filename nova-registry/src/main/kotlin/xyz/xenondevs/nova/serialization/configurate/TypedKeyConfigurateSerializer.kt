package xyz.xenondevs.nova.serialization.configurate

import io.leangen.geantyref.TypeToken
import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.TypedKey
import net.kyori.adventure.key.Key
import org.bukkit.Keyed
import org.spongepowered.configurate.serialize.ScalarSerializer
import java.lang.reflect.Type
import java.util.function.Predicate

/**
 * Creates a [TypedKeyConfigurateSerializer] for the given [registryKey].
 */
inline fun <reified T : Keyed> TypedKeyConfigurateSerializer(
    registryKey: RegistryKey<T>
): TypedKeyConfigurateSerializer<T> =
    TypedKeyConfigurateSerializer(registryKey, geantyrefTypeTokenOf())

/**
 * A configurate serializer for [TypedKey] that serializes to a string of `namespace:value`.
 */
class TypedKeyConfigurateSerializer<T : Keyed>(
    private val registryKey: RegistryKey<T>,
    type: TypeToken<TypedKey<T>>
) : ScalarSerializer<TypedKey<T>>(type) {
    
    override fun deserialize(type: Type, obj: Any): TypedKey<T> {
        return TypedKey.create(registryKey, Key.key(obj.toString()))
    }
    
    override fun serialize(item: TypedKey<T>, typeSupported: Predicate<Class<*>>): Any {
        return item.key().asString()
    }
    
}

