package xyz.xenondevs.nova.data.resources.builder.index

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.nova.data.resources.ResourcePath
import xyz.xenondevs.nova.util.parseResourceLocation

internal object GuisIndexDeserializer {
    
    fun deserialize(namespace: String, json: JsonElement): Map<ResourceLocation, ResourcePath> {
        require(json is JsonObject)
        
        val map = HashMap<ResourceLocation, ResourcePath>()
        json.entrySet().forEach { (id, element) -> 
            val path = element.asString
            map[parseResourceLocation(id, namespace)] = ResourcePath.of("$path.png", namespace)
        }
        
        return map
    }
    
}