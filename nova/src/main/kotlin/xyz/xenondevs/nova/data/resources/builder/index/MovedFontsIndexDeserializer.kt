package xyz.xenondevs.nova.data.resources.builder.index

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import it.unimi.dsi.fastutil.ints.IntSet
import xyz.xenondevs.commons.gson.getAllInts
import xyz.xenondevs.commons.gson.getIntOrNull
import xyz.xenondevs.nova.data.resources.ResourcePath

internal object MovedFontsIndexDeserializer {
    
    fun deserialize(namespace: String, json: JsonElement): Map<ResourcePath, IntSet> {
        require(json is JsonObject)
        
        val map = HashMap<ResourcePath, IntSet>()
        
        json.entrySet().forEach { (font, obj) ->
            require(obj is JsonObject)
            
            val path = ResourcePath.of(font, namespace)
            val range = obj.getAsJsonArray("range").getAllInts().sorted()
            require(range.size == 2)
            val step = obj.getIntOrNull("step") ?: 1
            require(step > 0)
            
            map.getOrPut(path, ::IntOpenHashSet) += IntProgression.fromClosedRange(range[0], range[1], step)
        }
        
        return map
    }
    
}