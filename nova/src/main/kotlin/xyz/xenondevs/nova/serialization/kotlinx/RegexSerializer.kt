package xyz.xenondevs.nova.serialization.kotlinx

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.regex.PatternSyntaxException

internal object RegexSerializer : KSerializer<Regex> {
    
    override val descriptor = PrimitiveSerialDescriptor("xyz.xenondevs.nova.Regex", PrimitiveKind.STRING)
    
    override fun deserialize(decoder: Decoder): Regex {
        try {
            return Regex(decoder.decodeString())
        } catch (e: PatternSyntaxException) {
            throw SerializationException(e)
        }
    }
    
    override fun serialize(encoder: Encoder, value: Regex) {
        encoder.encodeString(value.pattern)
    }
    
}