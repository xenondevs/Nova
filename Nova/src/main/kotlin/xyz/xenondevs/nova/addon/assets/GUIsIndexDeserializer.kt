package xyz.xenondevs.nova.addon.assets

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import xyz.xenondevs.nova.util.addNamespace
import xyz.xenondevs.nova.util.addPrefix

object GUIsIndexDeserializer {
    
    fun deserialize(namespace: String, json: JsonElement): Map<String, String> {
        require(json is JsonObject)
        
        val map = HashMap<String, String>()
        json.entrySet().forEach { (id, element) -> 
            val path = element.asString
            map[id.addNamespace(namespace)] = path.addPrefix("$namespace:") + ".png"
        }
        
        return map
    }
    
}