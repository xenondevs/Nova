package xyz.xenondevs.nova.data.resources.builder.index

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import xyz.xenondevs.nova.data.resources.ResourcePath
import xyz.xenondevs.nova.util.addNamespace

internal object GUIsIndexDeserializer {
    
    fun deserialize(namespace: String, json: JsonElement): Map<String, ResourcePath> {
        require(json is JsonObject)
        
        val map = HashMap<String, ResourcePath>()
        json.entrySet().forEach { (id, element) -> 
            val path = element.asString
            map[id.addNamespace(namespace)] = ResourcePath.of("$path.png", namespace)
        }
        
        return map
    }
    
}