package xyz.xenondevs.nova.serialization.kotlinx

import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.tag.TagKey
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.bukkit.Keyed

/**
 * Open base class for serializers of [TagKey].
 * Serializes the key as a string of `#namespace:value`.
 */
open class TagKeySerializer<T : Keyed>(
    /**
     * The registry this serializer is for.
     */
    val registryKey: RegistryKey<T>
) : KSerializer<TagKey<T>> {
    
    final override val descriptor = PrimitiveSerialDescriptor(
        "xyz.xenondevs.nova.TagKeySerializer.${registryKey.key().asString()}",
        PrimitiveKind.STRING
    )
    
    final override fun serialize(encoder: Encoder, value: TagKey<T>) {
        encoder.encodeString("#${value.key().asString()}")
    }
    
    final override fun deserialize(decoder: Decoder): TagKey<T> {
        val str = decoder.decodeString()
        if (!str.startsWith("#"))
            throw SerializationException("TagKey must be prefixed with '#': $str")
        val key = KeySerializer.parseKey(str.substring(1))
        return TagKey.create(registryKey, key)
    }
    
}


