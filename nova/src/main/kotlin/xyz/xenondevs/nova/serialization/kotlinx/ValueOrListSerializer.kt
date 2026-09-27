package xyz.xenondevs.nova.serialization.kotlinx

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonTransformingSerializer

/**
 * Serializable type alias for [List] of [T] with [ValueOrListSerializer].
 */
typealias ValueOrList<T> = @Serializable(with = ValueOrListSerializer::class) List<T>

/**
 * [JsonTransformingSerializer] that accepts both a single [T] or an array of [T]
 * and returns a [List] during deserialization.
 * 
 * During serialization, it will write a single value if the list has only one element,
 * or an array if it has multiple elements.
 */
class ValueOrListSerializer<T>(
    dataSerializer: KSerializer<T>
) : JsonTransformingSerializer<List<T>>(ListSerializer(dataSerializer)) {
    
    override fun transformDeserialize(element: JsonElement): JsonElement =
        element as? JsonArray ?: JsonArray(listOf(element))
    
    override fun transformSerialize(element: JsonElement): JsonElement {
        require(element is JsonArray) // this serializer is used only with lists
        return element.singleOrNull() ?: element
    }
    
}