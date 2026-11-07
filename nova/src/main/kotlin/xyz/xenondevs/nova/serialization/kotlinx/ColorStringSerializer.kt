package xyz.xenondevs.nova.serialization.kotlinx

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.awt.Color

/**
 * Serializes [Color] from a string like `"#FF0000"`, `"0xFF0000"`, or a decimal string like `"16711680"`.
 * Uses [Color.decode] which handles all standard color string formats.
 */
internal object ColorStringSerializer : KSerializer<Color> {
    
    override val descriptor = PrimitiveSerialDescriptor("xyz.xenondevs.nova.Color", PrimitiveKind.STRING)
    
    override fun deserialize(decoder: Decoder): Color {
        return Color.decode(decoder.decodeString())
    }
    
    override fun serialize(encoder: Encoder, value: Color) {
        encoder.encodeString(String.format("#%06x", value.rgb and 0xFFFFFF))
    }
    
}
