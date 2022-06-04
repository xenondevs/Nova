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
    
    private val resourcePackDir = File(NOVA.dataFolder, "ResourcePack")
    val packDir = File(resourcePackDir, "pack")
    val assetsDir = File(packDir, "assets")
    val languageDir = File(assetsDir, "minecraft/lang")
    val guisFile = File(assetsDir, "nova/font/gui.json")
    val packMetaFile = File(packDir, "pack.mcmeta")
    
    private val contents = listOf(MaterialContent(this), GUIContent(this), LanguageContent(this))
    
    init {
        packDir.deleteRecursively()
        packDir.mkdirs()
    }
    
    fun create(): File {
        // Delete existing files
        packDir.deleteRecursively()
        packDir.mkdirs()
        
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
        val namespace = File(assetsDir, pack.namespace)
        
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
        
        packMetaFile.parentFile.mkdirs()
        packMetaFile.writeText(GSON.toJson(packMcmetaObj))
    }
    
    private fun createZip(): File {
        val parameters = ZipParameters().apply { isIncludeRootFolder = false }
        val file = File(resourcePackDir, "ResourcePack.zip")
        val zip = ZipFile(file)
        zip.addFolder(packDir, parameters)
        IOUtils.removeZipTimestamps(file)
        
        return file
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
