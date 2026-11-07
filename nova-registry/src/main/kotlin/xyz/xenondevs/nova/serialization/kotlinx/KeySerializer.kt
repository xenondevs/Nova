package xyz.xenondevs.nova.serialization.kotlinx

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.kyori.adventure.key.Key
import net.kyori.adventure.key.Key.key
import org.bukkit.NamespacedKey

/**
 * A serializer for [Key], serializing to a string of `namespace:value`.
 */
object KeySerializer : KSerializer<Key> {
    
    override val descriptor = PrimitiveSerialDescriptor("xyz.xenondevs.nova.KeySerializer", PrimitiveKind.STRING)
    
    override fun serialize(encoder: Encoder, value: Key) {
        encoder.encodeString(value.asString())
    }
    
    override fun deserialize(decoder: Decoder): Key {
        return key(decoder.decodeString())
    }
    
}

/**
 * A serializer for [NamespacedKey], serializing to a string of `namespace:value`.
 */
object NamespacedKeySerializer : KSerializer<NamespacedKey> {
    
    override val descriptor = PrimitiveSerialDescriptor("xyz.xenondevs.nova.NamespacedKeySerializer", PrimitiveKind.STRING)
    
    override fun serialize(encoder: Encoder, value: NamespacedKey) {
        encoder.encodeString(value.toString())
    }
    
    override fun deserialize(decoder: Decoder): NamespacedKey {
        val key = KeySerializer.deserialize(decoder) // go through Key for better exception messages
        return NamespacedKey(key.namespace(), key.value())
    }
    
}