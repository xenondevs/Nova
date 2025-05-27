package xyz.xenondevs.nova.serialization.kotlinx

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import xyz.xenondevs.commons.version.Version

internal object VersionSerializer : KSerializer<Version> {
    
    override val descriptor = PrimitiveSerialDescriptor("xyz.xenondevs.nova.Version", PrimitiveKind.STRING)
    
    override fun serialize(encoder: Encoder, value: Version) {
        encoder.encodeString(value.toString())
    }
    
    override fun deserialize(decoder: Decoder): Version {
        return Version(decoder.decodeString())
    }
    
}