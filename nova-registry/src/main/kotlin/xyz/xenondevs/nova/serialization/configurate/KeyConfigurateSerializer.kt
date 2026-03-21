package xyz.xenondevs.nova.serialization.configurate

import net.kyori.adventure.key.Key
import org.bukkit.NamespacedKey
import org.spongepowered.configurate.serialize.ScalarSerializer
import java.lang.reflect.Type
import java.util.function.Predicate

/**
 * A configurate serializer for [Key] that serializes to a string of `namespace:value`.
 */
object KeyConfigurateSerializer : ScalarSerializer<Key>(Key::class.java) {
    override fun deserialize(type: Type, obj: Any) = Key.key(obj.toString())
    override fun serialize(item: Key, typeSupported: Predicate<Class<*>>) = item.toString()
}

/**
 * A configurate serializer for [NamespacedKey] that serializes to a string of `namespace:value`.
 */
object NamespacedKeyConfigurateSerializer : ScalarSerializer<NamespacedKey>(NamespacedKey::class.java) {
    override fun deserialize(type: Type, obj: Any) =
        NamespacedKey.fromString(obj as String) ?: throw IllegalArgumentException("Invalid key: $obj")
    
    override fun serialize(item: NamespacedKey, typeSupported: Predicate<Class<*>>) = item.toString()
}

