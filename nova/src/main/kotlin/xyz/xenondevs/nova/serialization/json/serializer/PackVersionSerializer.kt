package xyz.xenondevs.nova.serialization.json.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.IntArraySerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer
import xyz.xenondevs.nova.resources.builder.data.PackMcMeta

/**
 * Serializes and deserializes [PackMcMeta.PackFormatConstraint] to/from an array of one or two integers.
 */
internal object PackVersionSerializer : KSerializer<PackMcMeta.PackFormatConstraint> {
    
    private val delegateSerializer = IntArraySerializer()
    override val descriptor = SerialDescriptor("xyz.xenondevs.nova.MinPackVersionSerializer", delegateSerializer.descriptor)
    
    override fun serialize(encoder: Encoder, value: PackMcMeta.PackFormatConstraint) {
        val arr = if (value.minor != null) intArrayOf(value.major, value.minor) else intArrayOf(value.major)
        delegateSerializer.serialize(encoder, arr)
    }
    
    override fun deserialize(decoder: Decoder): PackMcMeta.PackFormatConstraint {
        val arr = delegateSerializer.deserialize(decoder)
        return when(val size = arr.size) {
            1 -> PackMcMeta.PackFormatConstraint(arr[0], null)
            2 -> PackMcMeta.PackFormatConstraint(arr[0], arr[1])
            else -> throw SerializationException("Expected array size of 1 or 2, got $size")
        }
    }
    
}

/**
 * Can deserialize the following formats for a [PackMcMeta.PackFormatConstraint]:
 * 
 * - `version: 1` -> [1]
 * - `version: [1, 2]` -> [1, 2]
 */
internal object PackVersionMultiFormatSerializer : JsonTransformingSerializer<PackMcMeta.PackFormatConstraint>(PackVersionSerializer) {
    
    override fun transformDeserialize(element: JsonElement): JsonElement =
        when (element) {
            is JsonArray -> element
            is JsonPrimitive -> JsonArray(listOf(element))
            else -> throw SerializationException("Unexpected JSON element: $element")
        }
    
} 