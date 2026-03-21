package xyz.xenondevs.nova.serialization.kotlinx

import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.bukkit.Keyed
import org.bukkit.Registry
import xyz.xenondevs.nova.registry.NovaRegistry
import xyz.xenondevs.nova.registry.NovaRegistryElement

/**
 * Open base class for serializers of [NovaRegistry] elements.
 * Serializes the element's [NovaRegistryElement.key] as a string of `namespace:value`.
 */
open class NovaRegistryElementSerializer<T : NovaRegistryElement<T>>(
    /**
     * The registry this serializer is for.
     */
    val registry: NovaRegistry<T>
) : KSerializer<T> {
    
    final override val descriptor = PrimitiveSerialDescriptor(
        "xyz.xenondevs.nova.NovaRegistryElementSerializer.${registry.key.asString()}",
        PrimitiveKind.STRING
    )
    
    final override fun serialize(encoder: Encoder, value: T) {
        encoder.encodeString(value.key.toString())
    }
    
    final override fun deserialize(decoder: Decoder): T {
        val key = KeySerializer.deserialize(decoder)
        return registry.getValue(key)
            ?: throw SerializationException("No element under $key in registry ${registry.key.asString()}")
    }
    
}

/**
 * Open base class for serializers of [Registry] elements.
 * Serializes the element's [Keyed.key] as a string of `namespace:value`.
 */
open class PaperRegistryElementSerializer<T : Keyed>(
    /**
     * The registry this serializer is for.
     */
    val registryKey: RegistryKey<T>,
    /**
     * The registry access to retrieve the registry from.
     */
    val registryAccess: RegistryAccess = RegistryAccess.registryAccess()
) : KSerializer<T> {
    
    private val registry: Registry<T> by lazy { registryAccess.getRegistry(registryKey) }
    
    final override val descriptor = PrimitiveSerialDescriptor(
        "xyz.xenondevs.nova.PaperRegistryElementSerializer.${registryKey.key().asString()}",
        PrimitiveKind.STRING
    )
    
    final override fun serialize(encoder: Encoder, value: T) {
        encoder.encodeString(value.key.toString())
    }
    
    final override fun deserialize(decoder: Decoder): T {
        val key = KeySerializer.deserialize(decoder)
        return registry.get(key)
            ?: throw SerializationException("No element under $key in registry ${registryKey.key().asString()}")
    }
    
}