package xyz.xenondevs.nova.data.resources.builder.basepack

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.bukkit.Material
import xyz.xenondevs.nova.util.data.GSON
import xyz.xenondevs.nova.util.data.getInt
import xyz.xenondevs.nova.util.data.getString
import xyz.xenondevs.nova.util.data.set
import java.io.File
import java.nio.file.Path
import java.util.*

internal abstract class FileMerger(protected val basePacks: BasePacks, val path: Path) {
    
    constructor(basePacks: BasePacks, path: String) : this(basePacks, Path.of(path))
    
    abstract fun merge(source: File, destination: File)
    
}

internal class ModelFileMerger(basePacks: BasePacks) : FileMerger(basePacks, "assets/minecraft/models") {
    
    override fun merge(source: File, destination: File) {
        if (destination.exists()) {
            val sourceObj = JsonParser.parseReader(source.reader()) as? JsonObject ?: return
            val sourceOverrides = sourceObj.get("overrides") as? JsonArray ?: return
            
            val destObj = JsonParser.parseReader(destination.reader()) as? JsonObject
            val destOverrides = destObj?.get("overrides") as? JsonArray
            
            if (destOverrides != null) {
                destOverrides.addAll(sourceOverrides)
                val overrides = processOverrides(destination, destOverrides)
                destObj["overrides"] = overrides
                destination.writeText(GSON.toJson(destObj))
                
                return
            }
        }
        
        source.copyTo(destination)
        processOverrides(destination)
    }
    
    private fun processOverrides(file: File) {
        val obj = JsonParser.parseReader(file.reader()) as? JsonObject ?: return
        val array = obj.get("overrides") as? JsonArray ?: return
        processOverrides(file, array)
    }
    
    private fun processOverrides(file: File, array: JsonArray): JsonArray {
        val matName = file.nameWithoutExtension.uppercase()
        val material = Material.values().firstOrNull { it.name == matName } ?: return array
        
        val overrides = TreeMap<Int, String>()
        val occupiedModelData = basePacks.occupiedModelData.getOrPut(material, ::HashSet)
        
        array.forEach { element ->
            val (customModelData, model) = getModelConfig(element) ?: return@forEach
            
            overrides[customModelData] = model
            occupiedModelData += customModelData
        }
        
        val sortedArray = JsonArray()
        overrides.forEach { (customModelData, model) ->
            sortedArray.add(createModelDataEntry(customModelData, model))
        }
        
        return sortedArray
    }
    
    companion object {
        
        fun sortOverrides(array: JsonArray): JsonArray {
            val overrides = TreeMap<Int, String>()
            array.forEach {
                val (customModelData, model) = getModelConfig(it) ?: return@forEach
                overrides[customModelData] = model
            }
            
            val sortedArray = JsonArray()
            overrides.forEach { (customModelData, model) ->
                sortedArray.add(createModelDataEntry(customModelData, model))
            }
            
            return sortedArray
        }
        
        fun getModelConfig(element: JsonElement): Pair<Int, String>? {
            val obj = element as? JsonObject ?: return null
            val predicate = obj.get("predicate") as? JsonObject ?: return null
            val customModelData = predicate.getInt("custom_model_data") ?: return null
            val model = obj.getString("model") ?: return null
            
            return customModelData to model
        }
        
        fun createModelDataEntry(customModelData: Int, path: String): JsonObject {
            val entry = JsonObject()
            val predicate = JsonObject().also { entry.add("predicate", it) }
            predicate.addProperty("custom_model_data", customModelData)
            entry.addProperty("model", path)
            return entry
        }
        
    }
    
}

internal class LangFileMerger(basePacks: BasePacks) : FileMerger(basePacks, "assets/minecraft/lang") {
    
    override fun merge(source: File, destination: File) {
        if (!destination.exists()) {
            source.copyTo(destination)
            return
        }
        
        val sourceObj = JsonParser.parseReader(source.reader()) as? JsonObject ?: return
        val destObj = JsonParser.parseReader(destination.reader()) as? JsonObject ?: return
        
        sourceObj.entrySet().forEach { (key, value) -> if (!destObj.has(key)) destObj[key] = value }
        
        destination.writeText(GSON.toJson(destObj))
    }
    
}

internal class FontFileMerger(basePacks: BasePacks) : FileMerger(basePacks, "assets/minecraft/lang") {
    
    override fun merge(source: File, destination: File) {
        if (!destination.exists()) {
            source.copyTo(destination)
            return
        }
        
        val sourceObj = JsonParser.parseReader(source.reader()) as? JsonObject ?: return
        val destObj = JsonParser.parseReader(destination.reader()) as? JsonObject ?: return
        val sourceProviders = sourceObj.get("providers") as? JsonArray ?: return
        val destProviders = destObj.get("providers") as? JsonArray ?: return
        
        destProviders.addAll(sourceProviders)
        
        destination.writeText(GSON.toJson(destObj))
    }
    
}