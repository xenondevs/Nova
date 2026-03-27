package xyz.xenondevs.nova.serialization.kotlinx

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.DoubleArraySerializer
import kotlinx.serialization.builtins.FloatArraySerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.joml.Vector3d
import org.joml.Vector3dc
import org.joml.Vector3f
import org.joml.Vector3fc
import org.joml.Vector4d
import org.joml.Vector4dc
import org.joml.Vector4f
import org.joml.Vector4fc

internal object Vector3fcAsArraySerializer : KSerializer<Vector3fc> {
    
    private val delegateSerializer = FloatArraySerializer()
    override val descriptor: SerialDescriptor = SerialDescriptor("xyz.xenondevs.nova.Vector3dcAsArraySerializer", delegateSerializer.descriptor)
    
    override fun serialize(encoder: Encoder, value: Vector3fc) {
        val data = floatArrayOf(value.x(), value.y(), value.z())
        delegateSerializer.serialize(encoder, data)
    }
    
    override fun deserialize(decoder: Decoder): Vector3fc {
        val array = decoder.decodeSerializableValue(delegateSerializer)
        return Vector3f(array[0], array[1], array[2])
    }
    
}

internal object Vector4fcAsArraySerializer : KSerializer<Vector4fc> {
    
    private val delegateSerializer = FloatArraySerializer()
    override val descriptor: SerialDescriptor = SerialDescriptor("xyz.xenondevs.nova.Vector3dcAsArraySerializer", delegateSerializer.descriptor)
    
    override fun serialize(encoder: Encoder, value: Vector4fc) {
        val data = floatArrayOf(value.x(), value.y(), value.z(), value.w())
        delegateSerializer.serialize(encoder, data)
    }
    
    override fun deserialize(decoder: Decoder): Vector4fc {
        val array = decoder.decodeSerializableValue(delegateSerializer)
        return Vector4f(array[0], array[1], array[2], array[3])
    }
    
}

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