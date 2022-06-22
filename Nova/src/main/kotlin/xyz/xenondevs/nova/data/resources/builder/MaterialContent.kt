package xyz.xenondevs.nova.data.resources.builder

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.bukkit.Material
import xyz.xenondevs.nova.addon.assets.AssetPack
import xyz.xenondevs.nova.addon.assets.ModelInformation
import xyz.xenondevs.nova.data.resources.Resources
import xyz.xenondevs.nova.data.resources.builder.basepack.ModelFileMerger
import xyz.xenondevs.nova.material.ModelData
import xyz.xenondevs.nova.util.data.GSON
import xyz.xenondevs.nova.util.mapToIntArray
import java.io.File
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

internal class MaterialContent(private val occupiedModelData: Map<Material, Set<Int>>) : PackContent {
    
    private val novaMaterials = HashMap<String, Pair<ModelInformation?, ModelInformation?>>()
    private val modelDataPosition = HashMap<Material, Int>()
    
    override fun addFromPack(pack: AssetPack) {
        val materialsIndex = pack.materialsIndex ?: return
        
        materialsIndex.forEach { mat ->
            val itemInfo = mat.itemInfo
            val blockInfo = mat.blockInfo
            
            novaMaterials[mat.id] = itemInfo to blockInfo
            
            itemInfo?.let { createDefaultModelFiles(pack, it) }
            blockInfo?.let { createDefaultModelFiles(pack, it) }
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
    
    override fun write() {
        val modelDataLookup = HashMap<String, Pair<ModelData?, ModelData?>>()
        val registeredMaterialModels = HashMap<Material, HashMap<String, Int>>()
        
        novaMaterials.forEach { (id, pair) ->
            val (itemInfo, blockInfo) = pair
            
            fun registerModels(info: ModelInformation): ModelData {
                val material = info.material
                val registeredModels = registeredMaterialModels.getOrPut(material, ::HashMap)
                
                val dataArray = info.models.mapToIntArray { model ->
                    registeredModels.getOrPut(model) { getNextModelData(material) }
                }
                
                return ModelData(material, dataArray, info.id)
            }
            
            val itemModelData = itemInfo?.let(::registerModels)
            val blockModelData = blockInfo?.let(::registerModels)
            
            modelDataLookup[id] = itemModelData to blockModelData
        }
        
        Resources.updateModelDataLookup(modelDataLookup)
        
        registeredMaterialModels.forEach { (material, registeredModels) ->
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
    }
    
    private fun getNextModelData(material: Material): Int {
        var pos = modelDataPosition.getOrPut(material) { 0 } + 1
        
        val occupiedSet = occupiedModelData[material]
        if (occupiedSet != null) {
            while (pos in occupiedSet) {
                pos++
            }
        }
        
        modelDataPosition[material] = pos
        
        return pos
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
    
}