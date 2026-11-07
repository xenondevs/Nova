package xyz.xenondevs.nova.serialization.kotlinx

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

internal object RegexSerializer : KSerializer<Regex> {
    
    override val descriptor = PrimitiveSerialDescriptor("xyz.xenondevs.nova.Regex", PrimitiveKind.STRING)
    
    override fun deserialize(decoder: Decoder): Regex {
        return Regex(decoder.decodeString())
    }
    
    override fun serialize(encoder: Encoder, value: Regex) {
        encoder.encodeString(value.pattern)
    }
    
}