package xyz.xenondevs.nova.serialization.kotlinx

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlinx.serialization.json.jsonPrimitive

/**
 * Wraps [KSerializer] to add a `$defaultNamespace:` prefix to non-namespaced [discriminator]s.
 */
internal class NamespacedPolymorphicSerializer<T : Any>(
    serializer: KSerializer<T>,
    private val defaultNamespace: String,
    private val discriminator: String = "type"
) : JsonTransformingSerializer<T>(serializer) {
    
    override fun transformDeserialize(element: JsonElement): JsonElement {
        if (element !is JsonObject)
            return element
        
        val type = element[discriminator]?.jsonPrimitive?.content
            ?: return element
        if (':' in type)
            return element
        
        return JsonObject(element + (discriminator to JsonPrimitive("$defaultNamespace:$type")))
    }
    
}
