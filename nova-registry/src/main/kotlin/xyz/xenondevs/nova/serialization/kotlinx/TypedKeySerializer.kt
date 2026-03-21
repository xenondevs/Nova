package xyz.xenondevs.nova.serialization.kotlinx

import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.TypedKey
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.bukkit.Keyed

/**
 * Open base class for serializers of [TypedKey].
 * Serializes the key as a string of `namespace:value`.
 */
open class TypedKeySerializer<T : Keyed>(
    private val registryKey: RegistryKey<T>
) : KSerializer<TypedKey<T>> {
    
    final override val descriptor = PrimitiveSerialDescriptor(
        "xyz.xenondevs.nova.TypedKeySerializer.${registryKey.key().asString()}",
        PrimitiveKind.STRING
    )
    
    final override fun serialize(encoder: Encoder, value: TypedKey<T>) {
        encoder.encodeString(value.key().asString())
    }
    
    final override fun deserialize(decoder: Decoder): TypedKey<T> {
        val key = KeySerializer.deserialize(decoder)
        return TypedKey.create(registryKey, key)
    }
    
}

