package xyz.xenondevs.nova.serialization.kotlinx

import com.mojang.math.MatrixUtil
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.FloatArraySerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import org.joml.Matrix3f
import org.joml.Matrix4f
import org.joml.Matrix4fc
import org.joml.Quaternionfc
import org.joml.Vector3f
import org.joml.Vector3fc

internal object Matrix4fcMultiFormatSerializer : KSerializer<Matrix4fc> {
    
    override val descriptor = Matrix4fcAsArraySerializer.descriptor
    
    override fun serialize(encoder: Encoder, value: Matrix4fc) {
        Matrix4fcAsArraySerializer.serialize(encoder, value)
    }
    
    override fun deserialize(decoder: Decoder): Matrix4fc {
        decoder as JsonDecoder
        val element = decoder.decodeJsonElement()
        val serializer = when {
            element is JsonArray -> Matrix4fcAsArraySerializer
            else -> Matrix4fcAsSingularValueDecompositionMultiFormatSerializer
        }
        return decoder.json.decodeFromJsonElement(serializer, element)
    }
    
}


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

internal object Matrix4fcAsSingularValueDecompositionMultiFormatSerializer : KSerializer<Matrix4fc> {
    
    override val descriptor = buildClassSerialDescriptor("xyz.xenondevs.nova.Matrix4fcAsSingularValueDecompositionMultiFormatSerializer") {
        element("right_rotation", QuaternionfMultiFormatSerializer.descriptor)
        element("scale", Vector3fcAsArraySerializer.descriptor)
        element("left_rotation", QuaternionfMultiFormatSerializer.descriptor)
        element("translation", Vector3fcAsArraySerializer.descriptor)
    }
    
    override fun serialize(encoder: Encoder, value: Matrix4fc) {
        val f = 1f / value.m33()
        val triple = MatrixUtil.svdDecompose(Matrix3f(value).scale(f))
        
        val translation = value.getTranslation(Vector3f()).mul(f)
        val leftRotation = triple.left
        val scale = triple.middle
        val rightRotation = triple.right
        
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, QuaternionfMultiFormatSerializer, rightRotation)
            encodeSerializableElement(descriptor, 1, Vector3fcAsArraySerializer, scale)
            encodeSerializableElement(descriptor, 2, QuaternionfMultiFormatSerializer, leftRotation)
            encodeSerializableElement(descriptor, 3, Vector3fcAsArraySerializer, translation)
        }
    }
    
    override fun deserialize(decoder: Decoder): Matrix4fc {
        return decoder.decodeStructure(descriptor) {
            var leftRotation: Quaternionfc? = null
            var rightRotation: Quaternionfc? = null
            var scale: Vector3fc? = null
            var translation: Vector3fc? = null
            
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> rightRotation = decodeSerializableElement(descriptor, 0, QuaternionfMultiFormatSerializer)
                    1 -> scale = decodeSerializableElement(descriptor, 1, Vector3fcAsArraySerializer)
                    2 -> leftRotation = decodeSerializableElement(descriptor, 2, QuaternionfMultiFormatSerializer)
                    3 -> translation = decodeSerializableElement(descriptor, 3, Vector3fcAsArraySerializer)
                    CompositeDecoder.DECODE_DONE -> break
                    else -> throw SerializationException("Unexpected index: $index")
                }
            }
            
            if (leftRotation == null)
                throw SerializationException("Transformation is missing 'left_rotation'")
            if (rightRotation == null)
                throw SerializationException("Transformation is missing 'right_rotation'")
            if (scale == null)
                throw SerializationException("Transformation is missing 'scale'")
            if (translation == null)
                throw SerializationException("Transformation is missing 'translation'")
            
            Matrix4f().apply {
                translate(translation)
                rotate(leftRotation)
                scale(scale)
                rotate(rightRotation)
            }
        }
    }
    
}