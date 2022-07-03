package xyz.xenondevs.nova.data.resources.builder

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.bukkit.Material
import xyz.xenondevs.nova.addon.assets.AssetPack
import xyz.xenondevs.nova.addon.assets.BlockModelInformation
import xyz.xenondevs.nova.addon.assets.ItemModelInformation
import xyz.xenondevs.nova.addon.assets.ModelInformation
import xyz.xenondevs.nova.data.resources.Resources
import xyz.xenondevs.nova.data.resources.builder.basepack.BasePacks
import xyz.xenondevs.nova.data.resources.builder.basepack.merger.ModelFileMerger
import xyz.xenondevs.nova.data.resources.model.config.BlockStateConfig
import xyz.xenondevs.nova.data.resources.model.config.BlockStateConfigType
import xyz.xenondevs.nova.data.resources.model.data.ArmorStandBlockModelData
import xyz.xenondevs.nova.data.resources.model.data.BlockModelData
import xyz.xenondevs.nova.data.resources.model.data.ItemModelData
import xyz.xenondevs.nova.data.resources.model.data.SolidBlockModelData
import xyz.xenondevs.nova.util.data.GSON
import xyz.xenondevs.nova.util.mapToArray
import xyz.xenondevs.nova.util.mapToIntArray
import java.io.File
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

internal class MaterialContent(private val basePacks: BasePacks) : PackContent {
    
    private val novaMaterials = HashMap<String, Pair<ItemModelInformation, BlockModelInformation>>()
    
    private val modelDataPosition = HashMap<Material, Int>()
    
    private val blockStatePosition = HashMap<BlockStateConfigType<*>, Int>()
    private val remainingBlockStates = HashMap<BlockStateConfigType<*>, Int>()
    
    override fun addFromPack(pack: AssetPack) {
        val materialsIndex = pack.materialsIndex ?: return
        
        materialsIndex.forEach { mat ->
            val itemInfo = mat.itemInfo
            val blockInfo = mat.blockInfo
            
            novaMaterials[mat.id] = itemInfo to blockInfo
            
            createDefaultModelFiles(pack, itemInfo)
        }
    }
    
    private fun createDefaultModelFiles(pack: AssetPack, info: ModelInformation) {
        info.models.forEach {
            val namespace = pack.namespace
            val file = File(ResourcePackBuilder.ASSETS_DIR, "$namespace/models/${it.removePrefix("$namespace:")}.json")
            if (!file.exists())
                createDefaultModelFile(file, it)
        }
    }
    
    private fun createDefaultModelFile(file: File, texturePath: String) {
        val modelObj = JsonObject()
        modelObj.addProperty("parent", "item/generated")
        modelObj.add("textures", JsonObject().apply { addProperty("layer0", texturePath) })
        
        file.parentFile.mkdirs()
        file.writeText(GSON.toJson(modelObj))
    }
    
    @Suppress("UNCHECKED_CAST")
    override fun write() {
        val modelDataLookup = HashMap<String, Pair<ItemModelData?, BlockModelData?>>()
        
        val customItemModels = HashMap<Material, HashMap<String, Int>>()
        val blockStateModels = HashMap<BlockStateConfigType<*>, HashMap<String, BlockStateConfig>>()
        
        novaMaterials.forEach { (id, pair) ->
            val info = pair.first
            val material = info.material
            
            val registeredModels = customItemModels.getOrPut(material, ::HashMap)
            val dataArray = info.models.mapToIntArray { model -> registeredModels.getOrPut(model) { getNextCustomModelData(material) } }
            modelDataLookup[id] = ItemModelData(info.id, material, dataArray) to null
        }
        
        novaMaterials.entries
            .sortedByDescending { it.value.second.priority }
            .forEach { (id, pair) ->
                val info = pair.second
                val itemModelData = modelDataLookup[id]!!.first
                
                val configType = info.type.configTypes.first { it == null || getRemainingBlockStateIdAmount(it) >= info.models.size }
                
                val blockModelData = if (configType == null) {
                    val material = ItemModelType.DEFAULT.material
                    val registeredModels = customItemModels.getOrPut(material, ::HashMap)
                    val dataArray = info.models.mapToIntArray { registeredModels.getOrPut(it) { getNextCustomModelData(material) } }
                    ArmorStandBlockModelData(id, info.hitboxType, material, dataArray)
                } else {
                    val registeredModels = blockStateModels.getOrPut(configType, ::HashMap)
                    val dataArray = info.models.mapToArray { registeredModels.getOrPut(it) { getNextBlockConfig(configType) } }
                    SolidBlockModelData(configType as BlockStateConfigType<BlockStateConfig>, id, dataArray)
                }
                
                modelDataLookup[id] = itemModelData to blockModelData
            }
        
        Resources.updateModelDataLookup(modelDataLookup)
        
        customItemModels.forEach { (material, registeredModels) ->
            val (file, modelObj, overrides) = getModelFile(material)
            
            registeredModels
                .toList()
                .sortedBy { it.second }
                .forEach { (path, customModelData) ->
                    overrides.add(ModelFileMerger.createModelDataEntry(customModelData, path))
                }
            
            modelObj.add("overrides", ModelFileMerger.sortOverrides(overrides))
            
            file.parentFile.mkdirs()
            file.writeText(GSON.toJson(modelObj))
        }
        
        blockStateModels.forEach { (type, registeredModels) ->
            val (file, mainObj, variants) = getBlockStateFile(type)
            
            registeredModels.forEach { (path, cfg) ->
                variants.add(cfg.variantString, JsonObject().apply { addProperty("model", path) })
            }
            
            file.parentFile.mkdirs()
            file.writeText(GSON.toJson(mainObj))
        }
    }
    
    private fun getNextCustomModelData(material: Material): Int {
        var pos = modelDataPosition.getOrPut(material) { 0 } + 1
        
        val occupiedSet = basePacks.occupiedModelData[material]
        if (occupiedSet != null) {
            while (pos in occupiedSet) {
                pos++
            }
        }
        
        modelDataPosition[material] = pos
        
        return pos
    }
    
    private fun <T : BlockStateConfig> getNextBlockConfig(type: BlockStateConfigType<T>): T {
        var pos = blockStatePosition.getOrPut(type) { -1 } + 1
        
        val occupiedSet = basePacks.occupiedSolidIds[type]
        val blockedSet = type.blockedIds
        
        while (pos in blockedSet || (occupiedSet != null && pos in occupiedSet)) {
            pos++
        }
        
        blockStatePosition[type] = pos
        remainingBlockStates[type] = remainingBlockStates[type]!! - 1
        
        return type.of(pos)
    }
    
    private fun getRemainingBlockStateIdAmount(type: BlockStateConfigType<*>): Int {
        return remainingBlockStates.getOrPut(type) {
            var count = 0
    
            val occupiedSet = basePacks.occupiedSolidIds[type]
            val blockedSet = type.blockedIds
    
            for (pos in 0..type.maxId) {
                if (pos in blockedSet || (occupiedSet != null && pos in occupiedSet)) continue
                count++
            }
            
            return@getOrPut count
        }
    }
    
    private fun getModelFile(material: Material): Triple<File, JsonObject, JsonArray> {
        val file = File(ResourcePackBuilder.ASSETS_DIR, "minecraft/models/item/${material.name.lowercase()}.json")
        if (!file.exists()) {
            val modelObj = JsonObject()
            
            // fixme: This does not cover all cases
            if (material.isBlock) {
                modelObj.addProperty("parent", "block/${material.name.lowercase()}")
            } else {
                modelObj.addProperty("parent", "item/generated")
                val textures = JsonObject().apply { addProperty("layer0", "item/${material.name.lowercase()}") }
                modelObj.add("textures", textures)
            }
            
            val overrides = JsonArray().also { modelObj.add("overrides", it) }
            
            return Triple(file, modelObj, overrides)
        } else {
            val modelObj = JsonParser.parseReader(file.reader()) as JsonObject
            val overrides = (modelObj.get("overrides") as? JsonArray) ?: JsonArray().also { modelObj.add("overrides", it) }
            
            return Triple(file, modelObj, overrides)
        }
    }
    
    private fun getBlockStateFile(type: BlockStateConfigType<*>): Triple<File, JsonObject, JsonObject> {
        val file = File(ResourcePackBuilder.ASSETS_DIR, "minecraft/blockstates/${type.fileName}.json")
        
        val mainObj: JsonObject
        val variants: JsonObject
        
        if (file.exists()) {
            mainObj = JsonParser.parseReader(file.reader()) as JsonObject
            variants = mainObj.getAsJsonObject("variants")
        } else {
            mainObj = JsonObject()
            variants = JsonObject()
            
            mainObj.add("variants", variants)
        }
        
        return Triple(file, mainObj, variants)
    }
    
}