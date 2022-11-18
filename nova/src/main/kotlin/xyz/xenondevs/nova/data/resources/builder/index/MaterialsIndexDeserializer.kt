package xyz.xenondevs.nova.data.resources.builder.index

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import org.bukkit.Material
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.data.resources.builder.content.material.info.BlockDirection
import xyz.xenondevs.nova.data.resources.builder.content.material.info.BlockModelInformation
import xyz.xenondevs.nova.data.resources.builder.content.material.info.BlockModelType
import xyz.xenondevs.nova.data.resources.builder.content.material.info.ItemModelInformation
import xyz.xenondevs.nova.data.resources.builder.content.material.info.RegisteredMaterial
import xyz.xenondevs.nova.util.addNamespace
import xyz.xenondevs.nova.util.data.getAllInts
import xyz.xenondevs.nova.util.data.getAllStrings
import xyz.xenondevs.nova.util.data.getInt
import xyz.xenondevs.nova.util.data.getOrNull
import xyz.xenondevs.nova.util.data.getString
import xyz.xenondevs.nova.util.data.isString

private fun String.toMaterial(): Material? = Material.getMaterial(uppercase())

internal object MaterialsIndexDeserializer {
    
    fun deserialize(namespace: String, json: JsonElement): List<RegisteredMaterial> {
        require(json is JsonObject)
        
        val index = ArrayList<RegisteredMaterial>()
        
        json.entrySet().forEach { (name, element) ->
            val itemInfo: ItemModelInformation?
            val blockInfo: BlockModelInformation?
            val armorInfo: NamespacedId?
            val id = NamespacedId(namespace, name)
            
            if (element is JsonObject) {
                val item = element.get("item")
                var itemMaterial: Material? = null
                var itemModelList: List<String>? = null
                if (item is JsonObject) {
                    itemMaterial = deserializeItemMaterial(item)
                    val models = item.get("model") ?: item.get("models")
                    if (models != null)
                        itemModelList = deserializeModelList(models)
                } else if (item.isString()) {
                    itemModelList = deserializeModelList(item)
                }
                
                itemInfo = itemModelList
                    ?.map { it.addNamespace(namespace) }
                    ?.let { ItemModelInformation(id, it, itemMaterial) }
                    ?: ItemModelInformation(id, emptyList())
                
                val block = element.getOrNull("block")
                blockInfo = if (block is JsonObject) {
                    (block.getOrNull("models")?.let(MaterialsIndexDeserializer::deserializeModelList) ?: itemModelList)
                        ?.map { it.addNamespace(namespace) }
                        ?.let {
                            val blockType = block.getString("type")?.uppercase()?.let(BlockModelType::valueOf)
                            val hitboxType = block.getString("hitbox")?.toMaterial()
                            val directions = block.getString("directions")?.let(BlockDirection::of)
                            val priority = block.getInt("priority", 0)
                            BlockModelInformation(id, blockType, hitboxType, it, directions, priority)
                        }
                } else null
                
                armorInfo = element.getString("armor")?.let { NamespacedId.of(it, namespace) }
                
            } else if (element.isString()) {
                itemInfo = ItemModelInformation(id, listOf(element.asString.addNamespace(namespace)))
                blockInfo = null
                armorInfo = null
            } else throw UnsupportedOperationException()
            
            index += RegisteredMaterial(id, itemInfo, blockInfo ?: itemInfo.toBlockInfo(), armorInfo)
        }
        
        return index
    }
    
    private fun deserializeModelList(element: JsonElement): List<String> {
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
        
        throw IllegalArgumentException("Could not deserialize model list: $element")
    }
    
    private fun deserializeItemMaterial(json: JsonObject): Material? =
        json.get("material")
            ?.takeUnless(JsonElement::isJsonNull)
            ?.asString?.uppercase()
            ?.let(Material::valueOf)
    
}