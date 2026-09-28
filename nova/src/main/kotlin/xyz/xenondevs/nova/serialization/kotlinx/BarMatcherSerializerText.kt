package xyz.xenondevs.nova.serialization.kotlinx

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import xyz.xenondevs.nova.ui.overlay.bossbar.positioning.BarMatcher
import xyz.xenondevs.nova.util.data.WildcardUtils

internal object BarMatcherSerializerText : JsonTransformingSerializer<BarMatcher.Text>(BarMatcher.Text.generatedSerializer()) {
    
    override fun transformDeserialize(element: JsonElement): JsonElement {
        val wildcard = (element as? JsonObject)?.get("wildcard")?.jsonPrimitive?.contentOrNull
            ?: return element
        return JsonObject(mapOf("regex" to JsonPrimitive(WildcardUtils.toRegexString(wildcard))))
    }
    
}