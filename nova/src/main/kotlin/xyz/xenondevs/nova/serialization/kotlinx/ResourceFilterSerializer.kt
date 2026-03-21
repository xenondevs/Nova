package xyz.xenondevs.nova.serialization.kotlinx

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import xyz.xenondevs.nova.resources.builder.ResourceFilter
import xyz.xenondevs.nova.util.data.WildcardUtils

/**
 * Serializes [ResourceFilter] with an additional `pattern_type` field to select whether `filter` is
 * a regex or wilcard pattern.
 */
internal object ResourceFilterSerializer : JsonTransformingSerializer<ResourceFilter>(ResourceFilter.generatedSerializer()) {
    
    override fun transformDeserialize(element: JsonElement): JsonElement {
        element as JsonObject
        val patternType = element["pattern_type"]?.jsonPrimitive?.contentOrNull
            ?: throw SerializationException("Missing 'pattern_type'")
        if (patternType == "regex") {
            return JsonObject(buildMap { 
                putAll(element)
                remove("pattern_type")
            })
        } else {
            val regex = element["filter"]?.jsonPrimitive?.contentOrNull?.let(WildcardUtils::toRegexString)
                ?: throw SerializationException("Missing 'filter'")
            return JsonObject(buildMap { 
                putAll(element)
                remove("pattern_type")
                put("filter", JsonPrimitive(regex))
            })
        }
    }
    
    override fun transformSerialize(element: JsonElement): JsonElement {
        return JsonObject(buildMap { 
            putAll(element as JsonObject)
            put("pattern_type", JsonPrimitive("regex"))
        })
    }
    
}
