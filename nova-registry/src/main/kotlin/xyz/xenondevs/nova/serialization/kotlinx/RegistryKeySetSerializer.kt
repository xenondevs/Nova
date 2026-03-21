package xyz.xenondevs.nova.serialization.kotlinx

import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.TypedKey
import io.papermc.paper.registry.set.RegistryKeySet
import io.papermc.paper.registry.set.RegistrySet
import io.papermc.paper.registry.tag.Tag
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

/**
 * Open base class for specialized [RegistryKeySet] serializers.
 * Serializes a tag to `#namespace:value` and a direct key set
 * to `["namespace:value1", "namespace:value2"]` (or just `"namespace:value"` if there's only one entry).
 *
 * This is a [JsonTransformingSerializer] as it needs to support both arrays and primitive values,
 * so it can only be used for JSON.
 */
open class RegistryKeySetSerializer<T : Keyed>(
    registryKey: RegistryKey<T>,
    registryAccess: RegistryAccess = RegistryAccess.registryAccess()
) : JsonTransformingSerializer<RegistryKeySet<T>>(
    BackingRegistryKeySetSerializer(registryKey, registryAccess)
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

private class BackingRegistryKeySetSerializer<T : Keyed>(
    private val registryKey: RegistryKey<T>,
    registryAccess: RegistryAccess
) : KSerializer<RegistryKeySet<T>> {
    
    private val registry by lazy { registryAccess.getRegistry(registryKey) }
    private val delegate = ListSerializer(String.serializer())
    override val descriptor = SerialDescriptor("xyz.xenondevs.nova.BackingRegistryKeySetSerializer", delegate.descriptor)
    
    override fun serialize(encoder: Encoder, value: RegistryKeySet<T>) {
        if (value is Tag<T>) {
            delegate.serialize(encoder, listOf("#${value.tagKey().key().asString()}"))
        } else {
            delegate.serialize(encoder, value.values().map { it.key().asString() })
        }
    }
    
    override fun deserialize(decoder: Decoder): RegistryKeySet<T> {
        val elements = delegate.deserialize(decoder)
        if (elements.size == 1 && elements[0].startsWith("#")) {
            val tagKey = TagKey.create(registryKey, KeySerializer.parseKey(elements[0].substring(1)))
            try {
                return registry.getTag(tagKey)
            } catch (e: NoSuchElementException) {
                throw SerializationException(e.message, e)
            }
        } else {
            if (elements.any { it.startsWith("#") })
                throw SerializationException("Cannot have multiple tags or values mixed with tags (got $elements)")
            return RegistrySet.keySet(registryKey, elements.map { TypedKey.create(registryKey, KeySerializer.parseKey(it)) })
        }
    }
    
}

