package xyz.xenondevs.nova.serialization.kotlinx

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.FloatArraySerializer
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import org.joml.AxisAngle4f
import org.joml.Quaternionf

internal object QuaternionfMultiFormatSerializer : KSerializer<Quaternionf> {
    
    override val descriptor = QuaternionfAsArraySerializer.descriptor
    
    override fun serialize(encoder: Encoder, value: Quaternionf) {
        QuaternionfAsArraySerializer.serialize(encoder, value)
    }
    
    override fun deserialize(decoder: Decoder): Quaternionf {
        decoder as JsonDecoder
        val element = decoder.decodeJsonElement()
        val serializer = when {
            element is JsonArray -> QuaternionfAsArraySerializer
            else -> QuaternionfAsAxisAngleSerializer
        }
        return decoder.json.decodeFromJsonElement(serializer, element)
    }
    
}

internal object QuaternionfAsArraySerializer : KSerializer<Quaternionf> {
    
    private val delegate = FloatArraySerializer()
    override val descriptor = delegate.descriptor
    
    override fun serialize(encoder: Encoder, value: Quaternionf) {
        delegate.serialize(encoder, floatArrayOf(value.x, value.y, value.z, value.w))
    }
    
    override fun deserialize(decoder: Decoder): Quaternionf {
        val arr = delegate.deserialize(decoder)
        if (arr.size != 4) 
            throw SerializationException("Expected array of size 4 for Quaternionf, but got ${arr.size}")
        return Quaternionf(arr[0], arr[1], arr[2], arr[3])
    }
    
}

internal object QuaternionfAsAxisAngleSerializer : KSerializer<Quaternionf> {
    
    private val arraySerializer = FloatArraySerializer()
    
    override val descriptor = buildClassSerialDescriptor("xyz.xenondevs.nova.QuaternionfAsAxisAngle") {
        element<Float>("angle")
        element("y", arraySerializer.descriptor)
    }
    
    override fun serialize(encoder: Encoder, value: Quaternionf) {
        val aa = AxisAngle4f()
        value.get(aa)
        
        encoder.encodeStructure(descriptor) {
            encodeFloatElement(descriptor, 0, aa.angle)
            encodeSerializableElement(descriptor, 1, arraySerializer, floatArrayOf(aa.x, aa.y, aa.z))
        }
    }
    
    override fun deserialize(decoder: Decoder): Quaternionf {
        var angle: Float? = null
        var axis: FloatArray? = null
        
        decoder.decodeStructure(descriptor) {
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> angle = decodeFloatElement(descriptor, 0)
                    1 -> axis = decodeSerializableElement(descriptor, 1, arraySerializer)
                    CompositeDecoder.DECODE_DONE -> break
                    else -> throw SerializationException("Unexpected index $index")
                }
            }
        }
        
        if (angle == null) 
            throw SerializationException("Missing angle for Quaternionf")
        if (axis == null) 
            throw SerializationException("Missing axis for Quaternionf")
        if (axis.size != 3) 
            throw SerializationException("Expected axis array of size 3, but got ${axis.size}")
        
        val aa = AxisAngle4f(axis[0], axis[1], axis[2], angle)
        return Quaternionf(aa)
    }
    
}