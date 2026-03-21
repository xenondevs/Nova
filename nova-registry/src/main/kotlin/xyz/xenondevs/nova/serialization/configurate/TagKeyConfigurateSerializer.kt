package xyz.xenondevs.nova.serialization.configurate

import io.leangen.geantyref.TypeToken
import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.tag.TagKey
import net.kyori.adventure.key.Key
import org.bukkit.Keyed
import org.spongepowered.configurate.serialize.ScalarSerializer
import java.lang.reflect.Type
import java.util.function.Predicate

/**
 * Creates a [TagKeyConfigurateSerializer] for the given [registryKey].
 */
inline fun <reified T : Keyed> TagKeyConfigurateSerializer(
    registryKey: RegistryKey<T>
): TagKeyConfigurateSerializer<T> =
    TagKeyConfigurateSerializer(registryKey, geantyrefTypeTokenOf())

/**
 * A configurate serializer for [TagKey] that serializes to a string of `#namespace:value`.
 */
class TagKeyConfigurateSerializer<T : Keyed>(
    private val registryKey: RegistryKey<T>,
    type: TypeToken<TagKey<T>>
) : ScalarSerializer<TagKey<T>>(type) {
    
    override fun deserialize(type: Type, obj: Any): TagKey<T> {
        val str = obj.toString()
        require(str.startsWith("#")) { "TagKey must be prefixed with '#': $str" }
        return TagKey.create(registryKey, Key.key(str.substring(1)))
    }
    
    override fun serialize(item: TagKey<T>, typeSupported: Predicate<Class<*>>): Any {
        return "#${item.key().asString()}"
    }
    
}


