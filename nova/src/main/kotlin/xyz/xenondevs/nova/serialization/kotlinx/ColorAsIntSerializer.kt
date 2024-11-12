package xyz.xenondevs.nova.serialization.kotlinx

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.awt.Color

internal object ColorAsIntSerializer : KSerializer<Color> {
    
    override val descriptor = PrimitiveSerialDescriptor("nova.ColorAsIntSerializer", PrimitiveKind.INT)
    
    override fun serialize(encoder: Encoder, value: Color) {
        encoder.encodeInt(value.rgb)
    }
    
    override fun deserialize(decoder: Decoder): Color {
        return Color(decoder.decodeInt())
    }
    
}