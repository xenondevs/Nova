package xyz.xenondevs.nova.addon.assets

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import org.bukkit.Material
import xyz.xenondevs.nova.data.resources.builder.BlockModelType
import xyz.xenondevs.nova.data.resources.builder.ItemModelType
import xyz.xenondevs.nova.util.addNamespace
import xyz.xenondevs.nova.util.data.*

private fun String.toMaterial(): Material? = Material.getMaterial(uppercase())

internal object MaterialsIndexDeserializer {
    
    fun deserialize(namespace: String, json: JsonElement): List<RegisteredMaterial> {
        require(json is JsonObject)
        
        val index = ArrayList<RegisteredMaterial>()
        
        json.entrySet().forEach { (name, element) ->
            val itemInfo: ItemModelInformation?
            val blockInfo: BlockModelInformation?
            val id = name.addNamespace(namespace)
            
            if (element is JsonObject) {
                val itemModelList = deserializeModelList(element, "item")
                itemInfo = itemModelList
                    ?.map { it.addNamespace(namespace) }
                    ?.let { ItemModelInformation(id, deserializeItemModelType(element), it) }
                    ?: ItemModelInformation(id, ItemModelType.DEFAULT.material, emptyList())
                
                blockInfo = (deserializeModelList(element, "block") ?: itemModelList)
                    ?.map { it.addNamespace(namespace) }
                    ?.let {
                        val priority = element.getInt("block_priority", 0)
                        val hitboxType = element.getString("block_hitbox")?.toMaterial()
                        val blockType = element.getString("block_type")?.uppercase()?.let(BlockModelType::valueOf) ?: BlockModelType.DEFAULT
                        BlockModelInformation(id, blockType, hitboxType, it, priority)
                    }
            } else {
                itemInfo = ItemModelInformation(id, ItemModelType.DEFAULT.material, listOf(element.asString.addNamespace(namespace)))
                blockInfo = null
            }
            
            index += RegisteredMaterial(id, itemInfo, blockInfo ?: itemInfo.toBlockInfo())
        }
        
        return index
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
    
    private fun deserializeItemModelType(json: JsonObject): Material {
        val material = json.get("item_material")
            ?.takeUnless(JsonElement::isJsonNull)
            ?.asString?.uppercase()
            ?.let(Material::valueOf)
        
        if (material != null) {
            return material
        }
        
        val itemModelType = json.get("item_type")
            ?.takeUnless(JsonElement::isJsonNull)
            ?.asString?.uppercase()
            ?.let(ItemModelType::valueOf)
            ?: ItemModelType.DEFAULT
        
        return itemModelType.material
    }
    
}

internal class RegisteredMaterial(
    val id: String,
    val itemInfo: ItemModelInformation,
    val blockInfo: BlockModelInformation
)

internal interface ModelInformation {
    val id: String
    val models: List<String>
}

internal class ItemModelInformation(
    override val id: String,
    val material: Material,
    override val models: List<String>
) : ModelInformation {
    
    fun toBlockInfo() = BlockModelInformation(id, BlockModelType.DEFAULT, null, models, 0)
    
}

internal class BlockModelInformation(
    override val id: String,
    val type: BlockModelType,
    hitboxType: Material?,
    override val models: List<String>,
    val priority: Int
) : ModelInformation {
    
    val hitboxType = hitboxType ?: DEFAULT_HITBOX_TYPE
    
    companion object {
        val DEFAULT_HITBOX_TYPE = Material.BARRIER
    }
    
}