package xyz.xenondevs.nova.data.resources.builder.index

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import xyz.xenondevs.commons.gson.getAllInts
import xyz.xenondevs.commons.gson.getIntOrNull
import xyz.xenondevs.nova.data.resources.ResourcePath

object MovedFontsIndexDeserializer {
    
    fun deserialize(namespace: String, json: JsonElement): Map<ResourcePath, Set<Int>> {
        require(json is JsonObject)
        
        val map = HashMap<ResourcePath, HashSet<Int>>()
        
        json.entrySet().forEach { (font, obj) ->
            require(obj is JsonObject)
            
            val path = ResourcePath.of(font, namespace)
            val range = obj.getAsJsonArray("range").getAllInts().sorted()
            require(range.size == 2)
            val step = obj.getIntOrNull("step") ?: 1
            require(step > 0)
            
            map.getOrPut(path, ::HashSet) += IntProgression.fromClosedRange(range[0], range[1], step)
        }
        
        return map
    }
    
}