package xyz.xenondevs.nova.serialization.kotlinx

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import xyz.xenondevs.nova.resources.ResourcePath

internal object ResourcePathAsStringSerializer : KSerializer<ResourcePath> {
    
    override val descriptor = PrimitiveSerialDescriptor("nova.ResourcePathAsStringSerializer", PrimitiveKind.STRING)
    
    override fun serialize(encoder: Encoder, value: ResourcePath) {
        encoder.encodeString(value.toString())
    }
    
    override fun deserialize(decoder: Decoder): ResourcePath {
        return ResourcePath.of(decoder.decodeString())
    }
}