package xyz.xenondevs.nova.data.resources.builder

import com.google.gson.JsonObject
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import org.bukkit.Material
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.addon.assets.AssetPack
import xyz.xenondevs.nova.data.config.DEFAULT_CONFIG
import xyz.xenondevs.nova.data.config.configReloadable
import xyz.xenondevs.nova.util.data.GSON
import xyz.xenondevs.nova.util.data.IOUtils
import xyz.xenondevs.nova.util.data.getIntOrNull
import java.io.File

@Suppress("MemberVisibilityCanBePrivate")
internal class ResourcePackBuilder(private val packs: List<AssetPack>) {
    
    private val contents = listOf(MaterialContent(this), GUIContent(this), LanguageContent(this))
    
    init {
        PACK_DIR.deleteRecursively()
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
        packObj.addProperty("pack_format", 8)
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
    
    companion object {
        
        private val RESOURCE_PACK_DIR = File(NOVA.dataFolder, "ResourcePack")
        val PACK_DIR = File(RESOURCE_PACK_DIR, "pack")
        val ASSETS_DIR = File(PACK_DIR, "assets")
        val LANGUAGE_DIR = File(ASSETS_DIR, "minecraft/lang")
        val GUIS_FILE = File(ASSETS_DIR, "nova/font/gui.json")
        val PACK_MCMETA_FILE = File(PACK_DIR, "pack.mcmeta")
        
        val RESOURCE_PACK_FILE = File(RESOURCE_PACK_DIR, "ResourcePack.zip")
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
    
}
