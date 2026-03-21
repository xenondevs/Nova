package xyz.xenondevs.nova.serialization.kotlinx

import io.papermc.paper.registry.TypedKey
import io.papermc.paper.registry.set.RegistryKeySet
import io.papermc.paper.registry.tag.TagKey
import kotlinx.serialization.modules.SerializersModuleBuilder
import xyz.xenondevs.nova.registry.RegistryEntry
import xyz.xenondevs.nova.registry.RegistryEntrySet
import kotlin.reflect.KClass

/**
 * Registers contextual serializers for [RegistryEntry], [RegistryEntrySet], [RegistryKeySet], [TagKey], and [TypedKey]
 * that resolve the target registries based on the configured element serializers.
 * 
 * Note that this only works when resolving the serializer at runtime (e.g., via [kotlinx.serialization.serializer])
 * as otherwise the element serializer will be a [kotlinx.serialization.ContextualSerializer] and thus it won't be
 * possible to infer the target registry.
 * This means that for properties of `@Serializable` classes, the serializer must be specified statically (e.g., via `@Serializable(with = ...)`)
 * instead of relying on contextual serialization.
 */
fun SerializersModuleBuilder.contextualRegistryElementBasedSerializers() {
    fun throwSerializerTypeMismatch(forClass: KClass<*>, expected: KClass<*>, actual: KClass<*>): Nothing =
        throw IllegalArgumentException(
            "Expected element serializer of ${forClass.qualifiedName} to be of type ${expected.qualifiedName}, but got ${actual.qualifiedName}. "
                + "Consider specifying the serializer statically instead of relying on contextual serialization."
        )
    
    contextual(RegistryEntry.Paper::class) { serializers ->
        val serializer = serializers.single() as? PaperRegistryElementSerializer<*>
            ?: throwSerializerTypeMismatch(RegistryEntry.Paper::class, PaperRegistryElementSerializer::class, serializers.single()::class)
        PaperRegistryEntrySerializer(serializer.registryKey, serializer.registryAccess)
    }
    
    contextual(RegistryEntry.Nova::class) { serializers ->
        val serializer = serializers.single() as? NovaRegistryElementSerializer<*>
            ?: throwSerializerTypeMismatch(RegistryEntry.Paper::class, NovaRegistryElementSerializer::class, serializers.single()::class)
        NovaRegistryEntrySerializer(serializer.registry)
    }
    
    contextual(RegistryEntry.Either::class) { serializers ->
        val novaSerializer = serializers[0] as? NovaRegistryElementSerializer<*>
            ?: throwSerializerTypeMismatch(RegistryEntry.Either::class, NovaRegistryElementSerializer::class, serializers[0]::class)
        val paperSerializer = serializers[1] as? PaperRegistryElementSerializer<*>
            ?: throwSerializerTypeMismatch(RegistryEntry.Either::class, PaperRegistryElementSerializer::class, serializers[1]::class)
        EitherRegistryEntrySerializer(novaSerializer.registry, paperSerializer.registryKey, paperSerializer.registryAccess)
    }
    
    contextual(RegistryEntrySet.Paper::class) { serializers ->
        val serializer = serializers.single() as? PaperRegistryElementSerializer<*>
            ?: throwSerializerTypeMismatch(RegistryEntrySet.Paper::class, PaperRegistryElementSerializer::class, serializers.single()::class)
        PaperRegistryEntrySetSerializer(serializer.registryKey, serializer.registryAccess)
    }
    
    contextual(RegistryEntrySet.Nova::class) { serializers ->
        val serializer = serializers.single() as? NovaRegistryElementSerializer<*>
            ?: throwSerializerTypeMismatch(RegistryEntrySet.Paper::class, NovaRegistryElementSerializer::class, serializers.single()::class)
        NovaRegistryEntrySetSerializer(serializer.registry)
    }
    
    contextual(RegistryEntrySet.Mixed::class) { serializers ->
        val novaSerializer = serializers[0] as? NovaRegistryElementSerializer<*>
            ?: throwSerializerTypeMismatch(RegistryEntrySet.Mixed::class, NovaRegistryElementSerializer::class, serializers[0]::class)
        val paperSerializer = serializers[1] as? PaperRegistryElementSerializer<*>
            ?: throwSerializerTypeMismatch(RegistryEntrySet.Mixed::class, PaperRegistryElementSerializer::class, serializers[1]::class)
        MixedRegistryEntrySetSerializer(novaSerializer.registry, paperSerializer.registryKey, paperSerializer.registryAccess)
    }
    
    contextual(RegistryKeySet::class) { serializers ->
        val serializer = serializers.single() as? PaperRegistryElementSerializer<*>
            ?: throwSerializerTypeMismatch(RegistryKeySet::class, PaperRegistryElementSerializer::class, serializers.single()::class)
        RegistryKeySetSerializer(serializer.registryKey, serializer.registryAccess)
    }
    
    contextual(TagKey::class) { serializers ->
        val serializer = serializers.single() as? PaperRegistryElementSerializer<*>
            ?: throwSerializerTypeMismatch(TagKey::class, PaperRegistryElementSerializer::class, serializers.single()::class)
        TagKeySerializer(serializer.registryKey)
    }
    
    contextual(TypedKey::class) { serializers ->
        val serializer = serializers.single() as? PaperRegistryElementSerializer<*>
            ?: throwSerializerTypeMismatch(TypedKey::class, PaperRegistryElementSerializer::class, serializers.single()::class)
        TypedKeySerializer(serializer.registryKey)
    }
}