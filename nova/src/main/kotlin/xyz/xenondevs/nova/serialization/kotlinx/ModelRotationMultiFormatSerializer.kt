package xyz.xenondevs.nova.serialization.kotlinx

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import xyz.xenondevs.nova.resources.builder.model.Model

object ModelRotationMultiFormatSerializer : JsonTransformingSerializer<Model.Element.Rotation>(Model.Element.Rotation.generatedSerializer()) {
    
    override fun transformDeserialize(element: JsonElement): JsonElement {
        val axis = (element as? JsonObject)
            ?.get("axis")
            ?.jsonPrimitive
            ?.contentOrNull
            ?: return element
        
        return JsonObject(buildMap {
            putAll(element)
            remove("axis")
            remove("angle")?.let { put(axis, it) }
        })
    }
    
    override fun transformSerialize(element: JsonElement): JsonElement {
        if (element !is JsonObject)
            return element
        
        var singleAxis: String? = null
        
        if ("x" in element)
            singleAxis = "x"
        
        if ("y" in element) {
            if (singleAxis != null)
                return element
            singleAxis = "y"
        }
        
        if ("z" in element) {
            if (singleAxis != null)
                return element
            singleAxis = "z"
        }
        
        if (singleAxis == null)
            return element
        
        return JsonObject(buildMap {
            putAll(element)
            put("axis", JsonPrimitive(singleAxis))
            put("angle", remove(singleAxis)!!)
        })
    }
    
}