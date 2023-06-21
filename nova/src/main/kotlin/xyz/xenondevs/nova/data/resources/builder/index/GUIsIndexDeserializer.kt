package xyz.xenondevs.nova.data.resources.builder.index

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import xyz.xenondevs.nova.data.resources.ResourcePath

internal object GuisIndexDeserializer {
    
    fun deserialize(namespace: String, json: JsonElement): Map<ResourcePath, ResourcePath> {
        require(json is JsonObject)
        
        val map = HashMap<ResourcePath, ResourcePath>()
        json.entrySet().forEach { (id, element) -> 
            val path = element.asString
            map[ResourcePath.of(id, namespace)] = ResourcePath.of("$path.png", namespace)
        }
        
        return map
    }
    
}