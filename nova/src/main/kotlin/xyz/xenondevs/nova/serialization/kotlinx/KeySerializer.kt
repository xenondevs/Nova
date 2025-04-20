package xyz.xenondevs.nova.serialization.kotlinx

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.kyori.adventure.key.Key

internal object KeySerializer : KSerializer<Key> {
    
    override val descriptor = PrimitiveSerialDescriptor("xyz.xenondevs.nova.KeySerializer", PrimitiveKind.STRING)
    
    override fun serialize(encoder: Encoder, value: Key) {
        encoder.encodeString(value.toString())
    }
    
    override fun deserialize(decoder: Decoder): Key {
        return Key.key(decoder.decodeString())
    }
    
}