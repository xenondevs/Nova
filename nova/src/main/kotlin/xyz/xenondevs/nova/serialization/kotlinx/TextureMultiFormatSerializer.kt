package xyz.xenondevs.nova.serialization.kotlinx

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer
import xyz.xenondevs.nova.resources.builder.model.Model

internal object ModelTextureMultiFormatSerializer : JsonTransformingSerializer<Model.Texture>(Model.Texture.generatedSerializer()) {
    
    override fun transformSerialize(element: JsonElement): JsonElement =
        if ((element as JsonObject).size == 1) element.values.single() else element
    
    override fun transformDeserialize(element: JsonElement): JsonElement {
        if (element is JsonPrimitive)
            return JsonObject(mapOf("sprite" to element))
        return element as? JsonObject
            ?: throw SerializationException("Expected JsonObject or JsonPrimitive, got ${element::class.java}")
    }
    
}