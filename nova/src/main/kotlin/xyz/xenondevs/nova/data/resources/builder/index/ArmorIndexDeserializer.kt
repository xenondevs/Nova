package xyz.xenondevs.nova.data.resources.builder.index

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import xyz.xenondevs.commons.gson.getDoubleOrNull
import xyz.xenondevs.commons.gson.getOrNull
import xyz.xenondevs.commons.gson.getStringOrNull
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.data.resources.ResourcePath
import xyz.xenondevs.nova.data.resources.builder.content.armor.info.ArmorEmissivityMapPath
import xyz.xenondevs.nova.data.resources.builder.content.armor.info.ArmorTexturePath
import xyz.xenondevs.nova.data.resources.builder.content.armor.info.RegisteredArmor
import xyz.xenondevs.nova.data.resources.builder.content.armor.info.RegisteredArmor.InterpolationMode

internal object ArmorIndexDeserializer {
    
    fun deserialize(namespace: String, json: JsonElement): List<RegisteredArmor> {
        require(json is JsonObject)
        
        val armor = ArrayList<RegisteredArmor>()
        json.entrySet().forEach { (idStr, element) ->
            val id = NamespacedId.of(idStr, namespace).resourceLocation
            require(id.namespace == namespace) { "Illegal namespace" }
            
            val layer1: String?
            var layer2: String? = null
            var layer1EmissivityMap: String? = null
            var layer2EmissivityMap: String? = null
            var interpolationMode = InterpolationMode.NONE
            var fps = 0.0
            
            when {
                element is JsonPrimitive && element.isString -> {
                    layer1 = element.asString
                }
                
                element is JsonArray -> {
                    require(element.size() in 1..2) { "Illegal layer amount" }
                    layer1 = element[0].asString
                    layer2 = element[1].asString
                }
                
                element is JsonObject -> {
                    layer1 = element.getStringOrNull("layer_1")
                    layer2 = element.getStringOrNull("layer_2")
                    layer1EmissivityMap = element.getStringOrNull("layer_1_emissivity_map")
                    layer2EmissivityMap = element.getStringOrNull("layer_2_emissivity_map")
                    
                    interpolationMode = element.getOrNull("interpolation")
                        ?.asString?.uppercase()
                        ?.let(InterpolationMode::valueOf)
                        ?: InterpolationMode.NONE
                    
                    fps = element.getDoubleOrNull("fps") ?: 0.0
                }
                
                else -> throw UnsupportedOperationException()
            }
            
            armor += RegisteredArmor(
                id,
                layer1?.let { ArmorTexturePath(ResourcePath.of(it, namespace)) },
                layer2?.let { ArmorTexturePath(ResourcePath.of(it, namespace)) },
                layer1EmissivityMap?.let { ArmorEmissivityMapPath(ResourcePath.of(it, namespace)) },
                layer2EmissivityMap?.let { ArmorEmissivityMapPath(ResourcePath.of(it, namespace)) },
                interpolationMode,
                fps,
            )
        }
        
        return armor
    }
    
}