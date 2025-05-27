package xyz.xenondevs.nova.serialization.kotlinx

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.FloatArraySerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.joml.Matrix4f
import org.joml.Matrix4fc

internal object Matrix4fcAsArraySerializer : KSerializer<Matrix4fc> {
    
    private val delegateSerializer = FloatArraySerializer()
    override val descriptor = SerialDescriptor("xyz.xenondevs.nova.Matrix4fcAsArray", delegateSerializer.descriptor)
    
    override fun serialize(encoder: Encoder, value: Matrix4fc) {
        val data = FloatArray(16)
        data[0] = value.m00()
        data[1] = value.m01()
        data[2] = value.m02()
        data[3] = value.m03()
        data[4] = value.m10()
        data[5] = value.m11()
        data[6] = value.m12()
        data[7] = value.m13()
        data[8] = value.m20()
        data[9] = value.m21()
        data[10] = value.m22()
        data[11] = value.m23()
        data[12] = value.m30()
        data[13] = value.m31()
        data[14] = value.m32()
        data[15] = value.m33()
        delegateSerializer.serialize(encoder, data)
    }
    
    override fun deserialize(decoder: Decoder): Matrix4fc {
        val m = decoder.decodeSerializableValue(delegateSerializer)
        return Matrix4f(
            m[0], m[1], m[2], m[3],
            m[4], m[5], m[6], m[7],
            m[8], m[9], m[10], m[11],
            m[12], m[13], m[14], m[15]
        )
    }
    
}