package xyz.xenondevs.nova.serialization.kotlinx

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

internal fun <T : Any> KSerializer<T>.nullOnFailure(): KSerializer<T?> {
    return NullableSerializerWrapper(this)
}

@OptIn(ExperimentalSerializationApi::class)
private class NullableSerializerWrapper<T : Any>(
    private val delegate: KSerializer<T>
) : KSerializer<T?> {
    
    override val descriptor = delegate.descriptor
    
    override fun deserialize(decoder: Decoder): T? {
        if (decoder.decodeNotNullMark()) {
            try {
                return delegate.deserialize(decoder)
            } catch (_: SerializationException) {
                return null
            }
        } else {
            decoder.decodeNull()
            return null
        }
    }
    
    override fun serialize(encoder: Encoder, value: T?) {
        if (value != null) {
            delegate.serialize(encoder, value)
        } else {
            encoder.encodeNull()
        }
    }
    
}