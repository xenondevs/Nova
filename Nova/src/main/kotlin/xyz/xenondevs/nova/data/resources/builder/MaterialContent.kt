package xyz.xenondevs.nova.data.resources.builder

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.bukkit.Material
import xyz.xenondevs.nova.addon.assets.AssetPack
import xyz.xenondevs.nova.addon.assets.ModelInformation
import xyz.xenondevs.nova.data.resources.Resources
import xyz.xenondevs.nova.material.ModelData
import xyz.xenondevs.nova.util.data.GSON
import xyz.xenondevs.nova.util.mapToIntArray
import java.io.File
import java.util.*

internal class MaterialContent(private val builder: ResourcePackBuilder) : PackContent {
    
    private val modelOverrides = HashMap<Material, TreeSet<String>>()
    private val toLookup = HashMap<String, Pair<ModelInformation?, ModelInformation?>>()
    
    override fun addFromPack(pack: AssetPack) {
        val materialsIndex = pack.materialsIndex ?: return
        
        // load all used models into the overrides map
        materialsIndex.forEach { mat ->
            val itemInfo = mat.itemInfo
            val blockInfo = mat.blockInfo
            
            if (itemInfo != null) loadInfo(itemInfo, pack.namespace)
            if (blockInfo != null) loadInfo(blockInfo, pack.namespace)
        }
        
        // fill the ModelData lookup map
        materialsIndex.forEach { mat ->
            val itemInfo = mat.itemInfo
            val blockInfo = mat.blockInfo
            
            toLookup[mat.id] = itemInfo to blockInfo
        }
    }
    
    private fun loadInfo(info: ModelInformation, namespace: String) {
        val material = info.materialType.material
        val modelList = modelOverrides.getOrPut(material) { TreeSet() }
        info.models.forEach {
            modelList += it
            
            // Create default item model file if no model file is present
            val file = File(builder.assetsDir, "$namespace/models/${it.removePrefix("$namespace:")}.json")
            if (!file.exists()) {
                val modelObj = JsonObject()
                modelObj.addProperty("parent", "item/generated")
                modelObj.add("textures", JsonObject().apply { addProperty("layer0", it) })
                
                file.parentFile.mkdirs()
                file.writeText(GSON.toJson(modelObj))
            }
        }
    }
    
    private fun createModelData(info: ModelInformation): ModelData {
        val material = info.materialType.material
        val modelDataStart = calculateModelDataStart(material)
        val sortedModelSet = modelOverrides[material]!!
        val dataArray = info.models.mapToIntArray { sortedModelSet.indexOf(it) + modelDataStart }
        
        return ModelData(info.materialType.material, dataArray, info.id, info.isBlock)
    }
    
    override fun write() {
        val modelDataLookup = toLookup.mapValuesTo(HashMap()) {
            it.value.first?.let(::createModelData) to it.value.second?.let(::createModelData)
        }
        Resources.updateModelDataLookup(modelDataLookup)
        
        modelOverrides.forEach { (material, models) ->
            val file = File(builder.assetsDir, "minecraft/models/item/${material.name.lowercase()}.json")
            val modelObj = JsonObject()
            
            // fixme: This does not cover all cases
            if (material.isBlock) {
                modelObj.addProperty("parent", "block/${material.name.lowercase()}")
            } else {
                modelObj.addProperty("parent", "item/generated")
                val textures = JsonObject().apply { addProperty("layer0", "item/${material.name.lowercase()}") }
                modelObj.add("textures", textures)
            }
            
            val overridesArr = JsonArray().also { modelObj.add("overrides", it) }
            
            var customModelData = calculateModelDataStart(material)
            models.forEach {
                val overrideObj = JsonObject().apply(overridesArr::add)
                overrideObj.add("predicate", JsonObject().apply { addProperty("custom_model_data", customModelData) })
                overrideObj.addProperty("model", it)
                
                customModelData++
            }
            
            file.parentFile.mkdirs()
            file.writeText(GSON.toJson(modelObj))
        }
    }
    
    private fun calculateModelDataStart(material: Material): Int =
        MaterialType.values()
            .filter { it.material == material }
            .maxOf { it.modelDataStart }
    
}