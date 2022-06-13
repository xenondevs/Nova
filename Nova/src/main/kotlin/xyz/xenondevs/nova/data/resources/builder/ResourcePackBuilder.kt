package xyz.xenondevs.nova.data.resources.builder

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import org.bukkit.Material
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.addon.assets.AssetPack
import xyz.xenondevs.nova.data.config.DEFAULT_CONFIG
import xyz.xenondevs.nova.data.config.configReloadable
import xyz.xenondevs.nova.util.StringUtils
import xyz.xenondevs.nova.util.data.GSON
import xyz.xenondevs.nova.util.data.IOUtils
import xyz.xenondevs.nova.util.data.getIntOrNull
import xyz.xenondevs.nova.util.data.set
import java.io.File
import java.nio.file.Path

internal class ResourcePackBuilder(private val packs: List<AssetPack>) {
    
    private val contents = listOf(MaterialContent(this), GUIContent(this), LanguageContent(this))
    
    init {
        PACK_DIR.deleteRecursively()
        BASE_PACKS_DIR.deleteRecursively()
        PACK_DIR.mkdirs()
    }
    
    fun create(): File {
        // Delete existing files
        PACK_DIR.deleteRecursively()
        PACK_DIR.mkdirs()
        
        // Include asset packs
        packs.forEach { pack ->
            LOGGER.info("Including asset pack ${pack.namespace}")
            copyBasicAssets(pack)
            contents.forEach { it.addFromPack(pack) }
        }
        
        // Write changes
        contents.forEach(PackContent::write)
        writeMetadata()
        
        // include base packs
        includeBasePacks()
        
        // Create a zip
        return createZip()
    }
    
    private fun copyBasicAssets(pack: AssetPack) {
        val namespace = File(ASSETS_DIR, pack.namespace)
        
        // Copy textures folder
        pack.texturesDir?.copyRecursively(File(namespace, "textures"))
        // Copy models folder
        pack.modelsDir?.copyRecursively(File(namespace, "models"))
        // Copy fonts folder
        pack.fontsDir?.copyRecursively(File(namespace, "font"))
        // Copy sounds folder
        pack.soundsDir?.copyRecursively(File(namespace, "sounds"))
    }
    
    private fun writeMetadata() {
        val packMcmetaObj = JsonObject()
        val packObj = JsonObject().also { packMcmetaObj.add("pack", it) }
        packObj.addProperty("pack_format", 9)
        packObj.addProperty("description", "Nova (${packs.size} AssetPacks loaded)")
        
        PACK_MCMETA_FILE.parentFile.mkdirs()
        PACK_MCMETA_FILE.writeText(GSON.toJson(packMcmetaObj))
    }
    
    private fun createZip(): File {
        val parameters = ZipParameters().apply { isIncludeRootFolder = false }
        val zip = ZipFile(RESOURCE_PACK_FILE)
        zip.addFolder(PACK_DIR, parameters)
        IOUtils.removeZipTimestamps(RESOURCE_PACK_FILE)
        
        return RESOURCE_PACK_FILE
    }
    
    private fun includeBasePacks() {
        BASE_PACKS.map {
            if (it.isFile) {
                val dir = File(BASE_PACKS_DIR, it.nameWithoutExtension + StringUtils.randomString(10))
                dir.mkdirs()
                ZipFile(it).extractAll(dir.absolutePath)
                
                return@map dir
            }
            
            return@map it
        }.forEach(::mergeBasePack)
    }
    
    private fun mergeBasePack(packDir: File) {
        LOGGER.info("Merging base pack $packDir")
        packDir.walkTopDown().forEach { file ->
            if (file.isDirectory) return@forEach
            
            val relStr = file.toRelativeString(packDir)
            val relPath = Path.of(relStr)
            val packFile = File(PACK_DIR, relStr)
            
            if (packFile.exists()) {
                when {
                    relPath.startsWith(Path.of("assets/minecraft/models")) -> mergeArrayFile(file, packFile, "overrides")
                    relPath.startsWith(Path.of("assets/minecraft/font")) -> mergeArrayFile(file, packFile, "providers")
                    relPath.startsWith(Path.of("assets/minecraft/lang")) -> mergeLangFile(file, packFile)
                    else -> LOGGER.warning("Skipping file $file")
                }
            } else file.copyTo(packFile)
        }
    }
    
    private fun mergeLangFile(source: File, destination: File) {
        val sourceObj = JsonParser.parseReader(source.reader()) as? JsonObject ?: return
        val destObj = JsonParser.parseReader(destination.reader()) as? JsonObject ?: return
        
        sourceObj.entrySet().forEach { (key, value) -> if (!destObj.has(key)) destObj[key] = value }
        
        destination.writeText(GSON.toJson(destObj))
    }
    
    private fun mergeArrayFile(source: File, destination: File, key: String) {
        val sourceObj = JsonParser.parseReader(source.reader()) as? JsonObject ?: return
        val destObj = JsonParser.parseReader(destination.reader()) as? JsonObject ?: return
        val sourceProviders = sourceObj.get(key) as? JsonArray ?: return
        val destProviders = destObj.get(key) as? JsonArray ?: return
        
        destProviders.addAll(sourceProviders)
        
        destination.writeText(GSON.toJson(destObj))
    }
    
    companion object {
        
        private val RESOURCE_PACK_DIR = File(NOVA.dataFolder, "ResourcePack")
        private val BASE_PACKS_DIR = File(RESOURCE_PACK_DIR, "BasePacks")
        val PACK_DIR = File(RESOURCE_PACK_DIR, "pack")
        val ASSETS_DIR = File(PACK_DIR, "assets")
        val LANGUAGE_DIR = File(ASSETS_DIR, "minecraft/lang")
        val GUIS_FILE = File(ASSETS_DIR, "nova/font/gui.json")
        val PACK_MCMETA_FILE = File(PACK_DIR, "pack.mcmeta")
        val RESOURCE_PACK_FILE = File(RESOURCE_PACK_DIR, "ResourcePack.zip")
        
        private val BASE_PACKS by configReloadable { DEFAULT_CONFIG.getStringList("resource_pack.base_packs").map(::File) }
        
    }
    
}

enum class MaterialType {
    
    DEFAULT,
    DAMAGEABLE,
    TRANSLUCENT,
    CONSUMABLE,
    ALWAYS_CONSUMABLE,
    FAST_CONSUMABLE;
    
    val material: Material by configReloadable { DEFAULT_CONFIG.getString("resource_pack.materials.${name.lowercase()}.type")!!.let { Material.valueOf(it.uppercase()) } }
    val modelDataStart: Int by configReloadable { DEFAULT_CONFIG.getIntOrNull("resource_pack.materials.${name.lowercase()}.modelDataStart")!! }
    
    companion object {
        val CUSTOM_DATA_START: Int by configReloadable { DEFAULT_CONFIG.getIntOrNull("resource_pack.materials.custom.modelDataStart")!! }
    }
    
}
