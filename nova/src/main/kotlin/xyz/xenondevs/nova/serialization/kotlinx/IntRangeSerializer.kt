package xyz.xenondevs.nova.serialization.kotlinx

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.IntArraySerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer

/**
 * Serializes [IntRange] as an array of two integers,  where the first integer is the minimum value
 * and the second integer is the maximum value, both inclusive.
 */
internal object IntRangeSerializer : KSerializer<IntRange> {
    
    private val delegateSerializer = IntArraySerializer()
    override val descriptor = SerialDescriptor("xyz.xenondevs.nova.IntRangeSerializer", delegateSerializer.descriptor)
    
    override fun serialize(encoder: Encoder, value: IntRange) {
        delegateSerializer.serialize(encoder, intArrayOf(value.first, value.last))
    }
    
    override fun deserialize(decoder: Decoder): IntRange {
        val array = delegateSerializer.deserialize(decoder)
        require(array.size == 2) { "Expected array of size 2, but got size ${array.size}" }
        return IntRange(array[0], array[1])
    }
    
}

/**
 * Can deserialize the following formats for an [IntRange]:
 *
 * * `range: 1` -> `[1, 1]`
 * * `range: [1, 2]` -> `[1, 2]`
 * * `range: { "min_inclusive": 1, "max_inclusive": 2 }` -> `[1, 2]`
 *
 */
internal object IntRangeMultiFormatSerializer : JsonTransformingSerializer<IntRange>(IntRangeSerializer) {
    
    override fun transformDeserialize(element: JsonElement): JsonElement =
        when (element) {
            is JsonArray -> element
            is JsonPrimitive -> JsonArray(listOf(element, element))
            is JsonObject -> {
                val min = element["min_inclusive"]
                    ?: throw NoSuchElementException("Missing 'min_inclusive' key in IntRange object")
                val max = element["max_inclusive"]
                    ?: throw NoSuchElementException("Missing 'max_inclusive' key in IntRange object")
                JsonArray(listOf(min, max))
            }
        }
    
}