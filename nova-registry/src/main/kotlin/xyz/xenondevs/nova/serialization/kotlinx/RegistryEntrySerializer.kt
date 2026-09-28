package xyz.xenondevs.nova.serialization.kotlinx

import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.TypedKey
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.bukkit.Keyed
import xyz.xenondevs.nova.registry.NovaRegistry
import xyz.xenondevs.nova.registry.NovaRegistryElement
import xyz.xenondevs.nova.registry.RegistryEntry

/**
 * Open base class for specialized [RegistryEntry.Nova] serializers.
 */
open class NovaRegistryEntrySerializer<T : NovaRegistryElement<T>>(
    /**
     * The registry this serializer is for.
     */
    val registry: NovaRegistry<T>
) : KSerializer<RegistryEntry.Nova<T>> {
    
    final override val descriptor = PrimitiveSerialDescriptor(
        "xyz.xenondevs.nova.NovaRegistryEntrySerializer.${registry.key.asString()}",
        PrimitiveKind.STRING
    )
    
    final override fun serialize(encoder: Encoder, value: RegistryEntry.Nova<T>) {
        encoder.encodeString(value.key.asString())
    }
    
    final override fun deserialize(decoder: Decoder): RegistryEntry.Nova<T> {
        val key = KeySerializer.deserialize(decoder)
        try {
            return registry[key]
        } catch (e: IllegalArgumentException) {
            throw SerializationException(e.message, e)
        }
    }
    
}

/**
 * Open base class for specialized [RegistryEntry.Paper] serializers.
 */
open class PaperRegistryEntrySerializer<T : Keyed>(
    /**
     * The registry this serializer is for.
     */
    val registryKey: RegistryKey<T>,
    /**
     * The registry access to retrieve the registry from.
     */
    val registryAccess: RegistryAccess = RegistryAccess.registryAccess()
) : KSerializer<RegistryEntry.Paper<T>> {
    
    final override val descriptor = PrimitiveSerialDescriptor(
        "xyz.xenondevs.nova.PaperRegistryEntrySerializer.${registryKey.key().asString()}",
        PrimitiveKind.STRING
    )
    
    final override fun serialize(encoder: Encoder, value: RegistryEntry.Paper<T>) {
        encoder.encodeString(value.key.asString())
    }
    
    final override fun deserialize(decoder: Decoder): RegistryEntry.Paper<T> {
        val key = KeySerializer.deserialize(decoder)
        try {
            return RegistryEntry.paper(TypedKey.create(registryKey, key), registryAccess)
        } catch (e: NoSuchElementException) {
            throw SerializationException(e.message, e)
        }
    }
    
}
