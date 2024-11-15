package xyz.xenondevs.nova.serialization.kotlinx

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.ResourceType

internal class ResourcePathSerializer<T : ResourceType>(
    contextSerializer: KSerializer<T>
) : KSerializer<ResourcePath<T>> {
    
    override val descriptor = PrimitiveSerialDescriptor("xyz.xenondevs.nova.ResourcePathSerializer", PrimitiveKind.STRING)
    private val resourceType: T by lazy { Json.decodeFromString(contextSerializer, "{}") }
    
    override fun serialize(encoder: Encoder, value: ResourcePath<T>) {
        encoder.encodeString(value.toString())
    }
    
    override fun deserialize(decoder: Decoder): ResourcePath<T> {
        return ResourcePath.of(resourceType, decoder.decodeString())
    }
    
}