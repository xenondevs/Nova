package xyz.xenondevs.nova.serialization.kotlinx

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.io.File

internal object FileSerializer : KSerializer<File> {
    
    override val descriptor = PrimitiveSerialDescriptor("xyz.xenondevs.nova.File", PrimitiveKind.STRING)
    
    override fun serialize(encoder: Encoder, value: File) {
        encoder.encodeString(value.path)
    }
    
    override fun deserialize(decoder: Decoder): File {
        return File(decoder.decodeString())
    }
    
}