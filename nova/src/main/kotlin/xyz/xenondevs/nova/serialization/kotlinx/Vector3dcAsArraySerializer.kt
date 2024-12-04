package xyz.xenondevs.nova.serialization.kotlinx

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.DoubleArraySerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.joml.Vector3d
import org.joml.Vector3dc
import org.joml.Vector4d
import org.joml.Vector4dc

@OptIn(ExperimentalSerializationApi::class)
internal object Vector3dcAsArraySerializer : KSerializer<Vector3dc> {
    
    private val delegateSerializer = DoubleArraySerializer()
    override val descriptor: SerialDescriptor = SerialDescriptor("xyz.xenondevs.nova.Vector3dcAsArraySerializer", delegateSerializer.descriptor)
    
    override fun serialize(encoder: Encoder, value: Vector3dc) {
        val data = doubleArrayOf(value.x(), value.y(), value.z())
        delegateSerializer.serialize(encoder, data)
    }
    
    override fun deserialize(decoder: Decoder): Vector3dc {
        val array = decoder.decodeSerializableValue(delegateSerializer)
        return Vector3d(array[0], array[1], array[2])
    }
    
}

@OptIn(ExperimentalSerializationApi::class)
internal object Vector4dcAsArraySerializer : KSerializer<Vector4dc> {
    
    private val delegateSerializer = DoubleArraySerializer()
    override val descriptor: SerialDescriptor = SerialDescriptor("xyz.xenondevs.nova.Vector3dcAsArraySerializer", delegateSerializer.descriptor)
    
    override fun serialize(encoder: Encoder, value: Vector4dc) {
        val data = doubleArrayOf(value.x(), value.y(), value.z(), value.w())
        delegateSerializer.serialize(encoder, data)
    }
    
    override fun deserialize(decoder: Decoder): Vector4dc {
        val array = decoder.decodeSerializableValue(delegateSerializer)
        return Vector4d(array[0], array[1], array[2], array[3])
    }
    
}