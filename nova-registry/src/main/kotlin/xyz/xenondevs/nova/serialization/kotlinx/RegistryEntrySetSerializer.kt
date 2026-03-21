package xyz.xenondevs.nova.serialization.kotlinx

import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.TypedKey
import io.papermc.paper.registry.tag.TagKey
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer
import org.bukkit.Keyed
import xyz.xenondevs.nova.registry.NovaRegistry
import xyz.xenondevs.nova.registry.NovaRegistryElement
import xyz.xenondevs.nova.registry.RegistryEntry
import xyz.xenondevs.nova.registry.RegistryEntrySet
import xyz.xenondevs.nova.registry.registryEntrySetOf

/**
 * Open base class for specialized [RegistryEntrySet.Nova] serializers.
 * Serializes [RegistryEntrySet.Nova.Tag] to `#namespace:value` and [RegistryEntrySet.Nova.Direct]
 * to `["namespace:value1", "namespace:value2"]` (or just `"namespace:value"` if there's only one entry).
 * 
 * This is a [JsonTransformingSerializer] as it needs to support both arrays and primitive values,
 * so it can only be used for JSON.
 */
open class NovaRegistryEntrySetSerializer<T : NovaRegistryElement<T>>(
    /**
     * The registry this serializer is for.
     */
    val registry: NovaRegistry<T>
) : JsonTransformingSerializer<RegistryEntrySet.Nova<T>>(
    BackingNovaRegistryElementSerializer(registry)
) {
    
    final override fun transformDeserialize(element: JsonElement): JsonElement {
        return when (element) {
            is JsonArray -> element
            is JsonPrimitive -> JsonArray(listOf(element))
            else -> throw SerializationException("Expected JsonArray or JsonPrimitive, but got ${element::class}")
        }
    }
    
    final override fun transformSerialize(element: JsonElement): JsonElement {
        if (element !is JsonArray)
            throw SerializationException("Expected JsonArray, but got ${element::class}")
        return if (element.size == 1) element[0] else element
    }
    
}

private class BackingNovaRegistryElementSerializer<T : NovaRegistryElement<T>>(
    private val registry: NovaRegistry<T>
) : KSerializer<RegistryEntrySet.Nova<T>> {
    
    private val delegate = ListSerializer(String.serializer())
    override val descriptor = SerialDescriptor("xyz.xenondevs.nova.BackingNovaRegistryElementSerializer", delegate.descriptor)
    
    override fun serialize(encoder: Encoder, value: RegistryEntrySet.Nova<T>) {
        when (value) {
            is RegistryEntrySet.Nova.Direct<T> -> delegate.serialize(encoder, value.entries.map { it.key.asString() })
            is RegistryEntrySet.Nova.Tag<T> -> delegate.serialize(encoder, listOf("#${value.tagKey.asString()}"))
        }
    }
    
    override fun deserialize(decoder: Decoder): RegistryEntrySet.Nova<T> {
        val elements = delegate.deserialize(decoder)
        if (elements.size == 1 && elements[0].startsWith("#")) {
            try {
                return registry.getTag(KeySerializer.parseKey(elements[0].substring(1)))
            } catch (e: IllegalArgumentException) {
                throw SerializationException(e.message, e)
            }
        } else {
            if (elements.any { it.startsWith("#") })
                throw SerializationException("Cannot have multiple tags or values mixed with tags (got $elements)")
            return registryEntrySetOf(elements.map {
                try {
                    registry[KeySerializer.parseKey(it)]
                } catch (e: IllegalArgumentException) {
                    throw SerializationException(e.message, e)
                }
            })
        }
    }
    
}

/**
 * Abstract base class for specialized [RegistryEntrySet.Paper] serializers.
 * Serializes [RegistryEntrySet.Paper.Tag] to `#namespace:value` and [RegistryEntrySet.Paper.Direct]
 * to `["namespace:value1", "namespace:value2"]` (or just `"namespace:value"` if there's only one entry).
 * 
 * This is a [JsonTransformingSerializer] as it needs to support both arrays and primitive values,
 * so it can only be used for JSON.
 */
open class PaperRegistryEntrySetSerializer<T : Keyed>(
    /**
     * The registry this serializer is for.
     */
    val registryKey: RegistryKey<T>,
    /**
     * The registry access to retrieve the registry from.
     */
    val registryAccess: RegistryAccess = RegistryAccess.registryAccess()
) : JsonTransformingSerializer<RegistryEntrySet.Paper<T>>(
    BackingPaperRegistryElementSerializer(registryKey, registryAccess)
) {
    
    final override fun transformDeserialize(element: JsonElement): JsonElement {
        return when (element) {
            is JsonArray -> element
            is JsonPrimitive -> JsonArray(listOf(element))
            else -> throw SerializationException("Expected JsonArray or JsonPrimitive, but got ${element::class}")
        }
    }
    
    final override fun transformSerialize(element: JsonElement): JsonElement {
        if (element !is JsonArray)
            throw SerializationException("Expected JsonArray, but got ${element::class}")
        return if (element.size == 1) element[0] else element
    }
    
}

private class BackingPaperRegistryElementSerializer<T : Keyed>(
    private val registry: RegistryKey<T>,
    private val registryAccess: RegistryAccess
) : KSerializer<RegistryEntrySet.Paper<T>> {
    
    private val delegate = ListSerializer(String.serializer())
    override val descriptor = SerialDescriptor("xyz.xenondevs.nova.BackingPaperRegistryElementSerializer", delegate.descriptor)
    
    override fun serialize(encoder: Encoder, value: RegistryEntrySet.Paper<T>) {
        when (value) {
            is RegistryEntrySet.Paper.Direct<T> -> delegate.serialize(encoder, value.entries.map { it.key.key().asString() })
            is RegistryEntrySet.Paper.Tag<T> -> delegate.serialize(encoder, listOf("#${value.tagKey.key().asString()}"))
        }
    }
    
    override fun deserialize(decoder: Decoder): RegistryEntrySet.Paper<T> {
        val elements = delegate.deserialize(decoder)
        try {
            if (elements.size == 1 && elements[0].startsWith("#")) {
                return registryEntrySetOf(TagKey.create(registry, KeySerializer.parseKey(elements[0].substring(1))), registryAccess)
            } else {
                if (elements.any { it.startsWith("#") })
                    throw SerializationException("Cannot have multiple tags or values mixed with tags (got $elements)")
                return registryEntrySetOf(elements.map { TypedKey.create(registry, KeySerializer.parseKey(it)) }, registryAccess)
            }
        } catch (e: NoSuchElementException) {
            throw SerializationException(e.message, e)
        }
    }
    
}

/**
 * Open base class for specialized [RegistryEntrySet.Mixed] serializers.
 * Serializes [RegistryEntrySet.Mixed.Tag] to `#namespace:value` and [RegistryEntrySet.Mixed.Direct]
 * to `["namespace:value1", "namespace:value2"]` (or just `"namespace:value"` if there's only one entry).
 * 
 * Nova registries will be prioritized for direct entries.
 * Tags will be merged from both registries.
 * 
 * This is a [JsonTransformingSerializer] as it needs to support both arrays and primitive values,
 * so it can only be used for JSON.
 */
open class MixedRegistryEntrySetSerializer<N : NovaRegistryElement<N>, P : Keyed>(
    /**
     * The Nova registry this serializer is for.
     */
    val novaRegistry: NovaRegistry<N>,
    /**
     * The Paper registry this serializer is for.
     */
    val paperRegistryKey: RegistryKey<P>,
    /**
     * The registry access to retrieve the Paper registry from.
     */
    val registryAccess: RegistryAccess = RegistryAccess.registryAccess()
) : JsonTransformingSerializer<RegistryEntrySet.Mixed<N, P>>(
    BackingMixedRegistryEntrySetSerializer(novaRegistry, paperRegistryKey, registryAccess)
) {
    
    final override fun transformDeserialize(element: JsonElement): JsonElement {
        return when (element) {
            is JsonArray -> element
            is JsonPrimitive -> JsonArray(listOf(element))
            else -> throw SerializationException("Expected JsonArray or JsonPrimitive, but got ${element::class}")
        }
    }
    
    final override fun transformSerialize(element: JsonElement): JsonElement {
        if (element !is JsonArray)
            throw SerializationException("Expected JsonArray, but got ${element::class}")
        return if (element.size == 1) element[0] else element
    }
    
}

private class BackingMixedRegistryEntrySetSerializer<N : NovaRegistryElement<N>, P : Keyed>(
    private val novaRegistry: NovaRegistry<N>,
    private val paperRegistry: RegistryKey<P>,
    private val registryAccess: RegistryAccess
) : KSerializer<RegistryEntrySet.Mixed<N, P>> {
    
    private val delegate = ListSerializer(String.serializer())
    override val descriptor = SerialDescriptor("xyz.xenondevs.nova.BackingMixedRegistryEntrySetSerializer", delegate.descriptor)
    
    override fun serialize(encoder: Encoder, value: RegistryEntrySet.Mixed<N, P>) {
        when (value) {
            is RegistryEntrySet.Mixed.Direct<N, P> -> delegate.serialize(encoder, value.entries.map { it.key.asString() })
            is RegistryEntrySet.Mixed.Tag<N, P> -> delegate.serialize(encoder, listOf("#${value.tagKey.key().asString()}"))
        }
    }
    
    override fun deserialize(decoder: Decoder): RegistryEntrySet.Mixed<N, P> {
        val elements = delegate.deserialize(decoder)
        try {
            if (elements.size == 1 && elements[0].startsWith("#")) {
                val tagKey = KeySerializer.parseKey(elements[0].substring(1))
                return registryEntrySetOf(tagKey, novaRegistry, paperRegistry, registryAccess)
            } else {
                if (elements.any { it.startsWith("#") })
                    throw SerializationException("Cannot have multiple tags or values mixed with tags (got $elements)")
                val entries = elements.map { RegistryEntry.either(KeySerializer.parseKey(it), novaRegistry, paperRegistry, registryAccess) }
                return registryEntrySetOf(entries, novaRegistry, paperRegistry)
            }
        } catch (e: NoSuchElementException) {
            throw SerializationException(e.message, e)
        }
    }
    
}
