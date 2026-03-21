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
    private val registry: NovaRegistry<T>
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
    private val registry: RegistryKey<T>,
    private val registryAccess: RegistryAccess = RegistryAccess.registryAccess()
) : KSerializer<RegistryEntry.Paper<T>> {
    
    final override val descriptor = PrimitiveSerialDescriptor(
        "xyz.xenondevs.nova.PaperRegistryEntrySerializer.${registry.key().asString()}",
        PrimitiveKind.STRING
    )
    
    final override fun serialize(encoder: Encoder, value: RegistryEntry.Paper<T>) {
        encoder.encodeString(value.key.asString())
    }
    
    final override fun deserialize(decoder: Decoder): RegistryEntry.Paper<T> {
        val key = KeySerializer.deserialize(decoder)
        return RegistryEntry.paper(TypedKey.create(registry, key), registryAccess)
    }
    
}

/**
 * Open base class for specialized [RegistryEntry.Either] serializers.
 * 
 * In case an entry exists in both the Nova and Paper registry, the Nova registry takes precedence.
 */
open class EitherRegistryEntrySerializer<N : NovaRegistryElement<N>, P : Keyed>(
    private val novaRegistry: NovaRegistry<N>,
    private val paperRegistry: RegistryKey<P>,
    private val registryAccess: RegistryAccess = RegistryAccess.registryAccess()
) : KSerializer<RegistryEntry.Either<N, P>> {
    
    final override val descriptor = PrimitiveSerialDescriptor(
        "xyz.xenondevs.nova.EitherRegistryEntrySerializer.${novaRegistry.key.asString()}",
        PrimitiveKind.STRING
    )
    
    final override fun serialize(encoder: Encoder, value: RegistryEntry.Either<N, P>) {
        encoder.encodeString(value.key.asString())
    }
    
    final override fun deserialize(decoder: Decoder): RegistryEntry.Either<N, P> {
        val key = KeySerializer.deserialize(decoder)
        return RegistryEntry.either(key, novaRegistry, paperRegistry, registryAccess)
    }
    
}
