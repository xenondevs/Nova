package xyz.xenondevs.nova.addon.assets

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import org.bukkit.Material
import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.data.resources.model.config.*
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
                val item = element.get("item")
                var modelType = ItemModelType.DEFAULT.material
                var itemModelList: List<String>? = null
                if (item is JsonObject) {
                    modelType = deserializeItemModelType(item)
                    if (item.has("models"))
                        itemModelList = deserializeModelList(item.get("models"))
                } else if (item.isString()) {
                    itemModelList = deserializeModelList(item)
                }
                
                itemInfo = itemModelList
                    ?.map { it.addNamespace(namespace) }
                    ?.let { ItemModelInformation(id, modelType, it) }
                    ?: ItemModelInformation(id, ItemModelType.DEFAULT.material, emptyList())
                
                val block = element.getOrNull("block")
                blockInfo = if (block is JsonObject) {
                    (block.getOrNull("models")?.let(::deserializeModelList) ?: itemModelList)
                        ?.map { it.addNamespace(namespace) }
                        ?.let {
                            val blockType = block.getString("type")?.uppercase()?.let(BlockModelType::valueOf)
                            val hitboxType = block.getString("hitbox")?.toMaterial()
                            val directions = block.getString("directions")?.let(BlockDirection::of)
                            val priority = block.getInt("priority", 0)
                            BlockModelInformation(id, blockType, hitboxType, it, directions, priority)
                        }
                } else null
                
            } else if (element.isString()) {
                itemInfo = ItemModelInformation(id, ItemModelType.DEFAULT.material, listOf(element.asString.addNamespace(namespace)))
                blockInfo = null
            } else throw UnsupportedOperationException()
            
            index += RegisteredMaterial(id, itemInfo, blockInfo ?: itemInfo.toBlockInfo())
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
    
    fun toBlockInfo() = BlockModelInformation(id, BlockModelType.DEFAULT, null, models, BlockDirection.values().toList(), 0)
    
}

internal class BlockModelInformation(
    override val id: String,
    type: BlockModelType?,
    hitboxType: Material?,
    override val models: List<String>,
    directions: List<BlockDirection>?,
    val priority: Int
) : ModelInformation {
    
    val type = type ?: BlockModelType.DEFAULT
    val hitboxType = hitboxType ?: DEFAULT_HITBOX_TYPE
    val directions = directions ?: listOf(BlockDirection.NORTH)
    
    companion object {
        val DEFAULT_HITBOX_TYPE = Material.BARRIER
    }
    
}

internal enum class ItemModelType(val material: Material) {
    
    DEFAULT(Material.SHULKER_SHELL),
    DAMAGEABLE(Material.FISHING_ROD),
    CONSUMABLE(Material.APPLE),
    ALWAYS_CONSUMABLE(Material.GOLDEN_APPLE),
    FAST_CONSUMABLE(Material.DRIED_KELP);
    
}

internal enum class BlockModelType(vararg val configTypes: BlockStateConfigType<*>?) {
    
    DEFAULT(null),
    SOLID(RedMushroomBlockStateConfig, BrownMushroomBlockStateConfig, MushroomStemBlockStateConfig, NoteBlockStateConfig, null);
    
}

internal enum class BlockDirection(val char: Char, val x: Int, val y: Int) {
    
    NORTH('n', 0, 0),
    EAST('e', 0, 90),
    SOUTH('s', 0, 180),
    WEST('w', 0, 270),
    UP('u', -90, 0),
    DOWN('d', 90, 0);
    
    val blockFace = BlockFace.valueOf(name)
    
    companion object {
        
        fun of(s: String): List<BlockDirection> {
            if (s.equals("all", true))
                return values().toList()
            
            return s.toCharArray().map { c -> BlockDirection.values().first { it.char == c } }
        }
        
    }
    
}