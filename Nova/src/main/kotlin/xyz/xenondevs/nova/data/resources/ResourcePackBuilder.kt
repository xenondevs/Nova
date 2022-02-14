package xyz.xenondevs.nova.data.resources

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.plotsquared.core.generator.HybridUtils.height
import net.lingala.zip4j.ZipFile
import org.bukkit.Material
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.addon.assets.AssetsPack
import xyz.xenondevs.nova.addon.assets.ModelInformation
import xyz.xenondevs.nova.data.config.DEFAULT_CONFIG
import xyz.xenondevs.nova.material.ModelData
import xyz.xenondevs.nova.util.data.GSON
import xyz.xenondevs.nova.util.data.addAll
import xyz.xenondevs.nova.util.mapToIntArray
import java.io.File
import java.util.*
import javax.imageio.ImageIO

class ResourcePackBuilder(private val packs: List<AssetsPack>) {
    
    private val directory = File(NOVA.dataFolder, "ResourcePack")
    private val assetsDir = File(directory, "assets")
    private val languageDir = File(assetsDir, "minecraft/lang")
    
    private val languages = HashMap<String, JsonObject>()
    private val guis = HashMap<String, Pair<Int, Int>>()
    private val modelOverrides = HashMap<Material, TreeSet<String>>()
    
    private val modelDataLookup = HashMap<String, Pair<ModelData?, ModelData?>>()
    private val guiLookup = HashMap<String, Triple<Char, Int, Int>>()
    
    init {
        directory.deleteRecursively()
        directory.mkdirs()
    }
    
    fun create(): File {
        // Delete existing files
        directory.deleteRecursively()
        directory.mkdirs()
        
        // Include asset packs
        packs.forEach { pack ->
            LOGGER.info("Including asset pack ${pack.namespace}")
            copyBasicAssets(pack)
            addLanguages(pack)
            loadMaterials(pack)
            loadGUIs(pack)
        }
        
        // Write changes
        writeOverrides()
        writeLanguages()
        writeGUIs()
        writeMetadata()
        
        // Update lookup maps
        Resources.updateLookupMaps(modelDataLookup, guiLookup)
        
        // Create a zip
        return createZip()
    }
    
    private fun copyBasicAssets(pack: AssetsPack) {
        val namespace = File(assetsDir, pack.namespace)
        
        // Copy textures folder
        pack.texturesDir?.copyRecursively(File(namespace, "textures"))
        // Copy models folder
        pack.modelsDir?.copyRecursively(File(namespace, "models"))
        // Copy fonts folder
        pack.fontsDir?.copyRecursively(File(namespace, "font"))
        // Copy sounds folder
        pack.soundsDir?.copyRecursively(File(namespace, "sounds"))
        // Copy guis
        pack.guisDir?.copyRecursively(File(namespace, "textures/gui/guis"))
    }
    
    private fun addLanguages(pack: AssetsPack) {
        // merge languages
        pack.langDir?.listFiles()?.forEach { lang ->
            if (lang.isFile && lang.endsWith(".json")) {
                val mainLangJsonObj = languages.getOrPut(lang.nameWithoutExtension) { JsonObject() }
                val packLangJsonObj = JsonParser.parseReader(lang.reader()) as JsonObject
                mainLangJsonObj.addAll(packLangJsonObj)
            }
        }
    }
    
    private fun loadMaterials(pack: AssetsPack) {
        // load all used models into the overrides map
        pack.assetsIndex.forEach { mat ->
            val itemInfo = mat.itemInfo
            val blockInfo = mat.blockInfo
            
            if (itemInfo != null) loadInfo(itemInfo, pack.namespace)
            if (blockInfo != null) loadInfo(blockInfo, pack.namespace)
        }
        
        // fill the ModelData lookup map
        pack.assetsIndex.forEach { mat ->
            val itemInfo = mat.itemInfo
            val blockInfo = mat.blockInfo
            
            val itemModelData = itemInfo?.let(::createModelData)
            val blockModelData = blockInfo?.let(::createModelData)
            
            modelDataLookup[mat.id] = itemModelData to blockModelData
        }
    }
    
    private fun loadInfo(info: ModelInformation, namespace: String) {
        val material = info.materialType.configuredMaterial
        val modelList = modelOverrides.getOrPut(material) { TreeSet() }
        info.models.forEach {
            modelList += it
    
    
            // Create default item model file if no model file is present
            val file = File(assetsDir, "$namespace/models/${it.removePrefix("$namespace:")}.json")
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
        val material = info.materialType.configuredMaterial
        val sortedModelSet = modelOverrides[material]!!
        val dataArray = info.models.mapToIntArray { sortedModelSet.indexOf(it) + 1 }
        
        return ModelData(material, dataArray)
    }
    
    private fun loadGUIs(pack: AssetsPack) {
        pack.guisDir?.walkTopDown()?.forEach {
            if (it.isDirectory) return@forEach
            val path = "gui/guis/" + pack.guisDir.toURI().relativize(it.toURI()).path
            val image = ImageIO.read(it)
            guis["${pack.namespace}:$path"] = image.width to image.height
        }
    }
    
    private fun writeOverrides() {
        modelOverrides.forEach { (material, models) ->
            val file = File(assetsDir, "minecraft/models/item/${material.name.lowercase()}.json")
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
            
            var customModelData = 1
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
    
    private fun writeGUIs() {
        val file = File(assetsDir, "nova/font/gui.json")
        
        val guiObj = JsonObject()
        val providers = JsonArray().also { guiObj.add("providers", it) }
        
        var char = '\uF000'
        guis.forEach { (path, bounds) ->
            val provider = JsonObject().apply(providers::add)
            
            provider.addProperty("file", path)
            provider.addProperty("height", bounds.second)
            provider.addProperty("ascent", 13)
            provider.addProperty("type", "bitmap")
            provider.add("chars", JsonArray().apply { add(char) })
            
            guiLookup[path] = Triple(char, bounds.first, bounds.second)
            
            char++
        }
        
        file.parentFile.mkdirs()
        file.writeText(GSON.toJson(guiObj))
    }
    
    private fun writeLanguages() {
        languages.forEach { (name, content) ->
            val file = File(languageDir, name)
            file.parentFile.mkdirs()
            file.writeText(GSON.toJson(content))
        }
    }
    
    private fun writeMetadata() {
        val file = File(directory, "pack.mcmeta")
        val packMcmetaObj = JsonObject()
        val packObj = JsonObject().also { packMcmetaObj.add("pack", it) }
        packObj.addProperty("pack_format", 8)
        packObj.addProperty("description", "Nova (${packs.size} AssetPacks loaded)")
        
        file.parentFile.mkdirs()
        file.writeText(GSON.toJson(packMcmetaObj))
    }
    
    private fun createZip(): File {
        val file = File(directory, "ResourcePack.zip")
        val zip = ZipFile(file)
        zip.addFolder(assetsDir)
        zip.addFile(File(directory, "pack.mcmeta"))
        
        return file
    }
    
}

enum class MaterialType {
    
    DEFAULT,
    DAMAGEABLE,
    TRANSLUCENT;
    
    val configuredMaterial = Material.valueOf(DEFAULT_CONFIG.getString("resource_pack.materials.${name.lowercase()}")!!.uppercase())
    
}
