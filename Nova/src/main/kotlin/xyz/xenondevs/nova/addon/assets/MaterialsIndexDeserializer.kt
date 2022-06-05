package xyz.xenondevs.nova.addon.assets

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import org.bukkit.Material
import xyz.xenondevs.nova.data.resources.builder.MaterialType
import xyz.xenondevs.nova.util.addNamespace
import xyz.xenondevs.nova.util.data.getAllInts
import xyz.xenondevs.nova.util.data.getAllStrings
import xyz.xenondevs.nova.util.data.isString

internal object MaterialsIndexDeserializer {
    
    fun deserialize(namespace: String, json: JsonElement): List<RegisteredMaterial> {
        require(json is JsonObject)
        
        val index = ArrayList<RegisteredMaterial>()
        
        json.entrySet().forEach { (name, element) ->
            val itemInfo: ModelInformation?
            val blockInfo: ModelInformation?
            val id = name.addNamespace(namespace)
            
            if (element is JsonObject) {
                itemInfo = deserializeModelList(element, "item")
                    ?.map { it.addNamespace(namespace) }
                    ?.let { ModelInformation(deserializeMaterialType(element, "item"), it, id) }
                blockInfo = deserializeModelList(element, "block")
                    ?.map { it.addNamespace(namespace) }
                    ?.let { ModelInformation(deserializeMaterialType(element, "block"), it, id) }
            } else {
                itemInfo = ModelInformation(MaterialType.DEFAULT.material, listOf(element.asString.addNamespace(namespace)), id)
                blockInfo = null
            }
            
            index += RegisteredMaterial(id, itemInfo ?: blockInfo, blockInfo ?: itemInfo)
        }
        
        return index
    }
    
    private fun deserializeMaterialType(json: JsonObject, path: String): Material {
        val material = json.get(path + "_material")
            ?.takeUnless(JsonElement::isJsonNull)
            ?.asString?.uppercase()
            ?.let(Material::valueOf)
        
        if (material != null) {
            return material
        }
        
        val materialType = json.get(path + "_type")
            ?.takeUnless(JsonElement::isJsonNull)
            ?.asString?.uppercase()
            ?.let(MaterialType::valueOf)
            ?: MaterialType.DEFAULT
        
        return materialType.material
    }
    
    private fun deserializeModelList(json: JsonObject, path: String): List<String>? {
        val element = json.get(path)?.takeUnless(JsonElement::isJsonNull) ?: return null
        
        if (element.isString()) return listOf(element.asString)
        if (element is JsonArray) return element.getAllStrings()
        
        if (element is JsonObject) {
            val models = ArrayList<String>()
            element.entrySet().forEach { (formatString, bounds) ->
                if (bounds !is JsonArray)
                    throw IllegalArgumentException("Bounds needs to be a JsonArray")
                val range = bounds.getAllInts()
                if (range.size != 2 || range[0] >= range[1])
                    throw IllegalArgumentException("Invalid bounds")
                
                for (i in range[0]..range[1]) {
                    models += formatString.format(i)
                }
            }
            
            return models
        }
        
        throw IllegalArgumentException("Could not deserialize model list: $json")
    }
    
}

internal data class RegisteredMaterial(val id: String, val itemInfo: ModelInformation?, val blockInfo: ModelInformation?)

internal data class ModelInformation(val material: Material, val models: List<String>, val id: String)