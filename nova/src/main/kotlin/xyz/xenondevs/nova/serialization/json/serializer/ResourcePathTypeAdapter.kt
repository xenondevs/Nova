package xyz.xenondevs.nova.serialization.json.serializer

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.ResourceType
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

internal object ResourcePathSerialization : JsonSerializer<ResourcePath<*>>, JsonDeserializer<ResourcePath<*>> {
    
    override fun serialize(src: ResourcePath<*>, typeOfSrc: Type, context: JsonSerializationContext?): JsonElement {
        return JsonPrimitive(src.toString())
    }
    
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext?): ResourcePath<*> {
        val resourceType = ((typeOfT as ParameterizedType).actualTypeArguments[0] as Class<*>).kotlin.objectInstance as ResourceType
        return ResourcePath.of(resourceType, json.asString)
    }
    
}