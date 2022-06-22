package xyz.xenondevs.nova.data.resources.builder

import com.google.gson.JsonObject
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import org.bukkit.Material
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.addon.AddonManager
import xyz.xenondevs.nova.addon.assets.AssetPack
import xyz.xenondevs.nova.data.resources.builder.basepack.BasePacks
import xyz.xenondevs.nova.util.data.GSON
import xyz.xenondevs.nova.util.data.IOUtils
import xyz.xenondevs.nova.util.data.write
import java.io.File

@Suppress("MemberVisibilityCanBePrivate")
internal object ResourcePackBuilder {
    
    val RESOURCE_PACK_DIR = File(NOVA.dataFolder, "resource_pack")
    val BASE_PACKS_DIR = File(RESOURCE_PACK_DIR, "base_packs")
    val ASSET_PACKS_DIR = File(RESOURCE_PACK_DIR, "asset_packs")
    val PACK_DIR = File(RESOURCE_PACK_DIR, "pack")
    val ASSETS_DIR = File(PACK_DIR, "assets")
    val LANGUAGE_DIR = File(ASSETS_DIR, "minecraft/lang")
    val GUIS_FILE = File(ASSETS_DIR, "nova/font/gui.json")
    val PACK_MCMETA_FILE = File(PACK_DIR, "pack.mcmeta")
    val RESOURCE_PACK_FILE = File(RESOURCE_PACK_DIR, "ResourcePack.zip")
    
    init {
        // delete legacy resource pack folder
        File(NOVA.dataFolder, "ResourcePack").deleteRecursively()
    }
    
    fun buildPack(): File {
        // Delete existing files
        RESOURCE_PACK_DIR.deleteRecursively()
        PACK_DIR.mkdirs()
        
        // extract files
        val basePacks = BasePacks().also(BasePacks::include)
        val assetPacks = extractAssetPacks()
        
        // init content
        val contents = listOf(
            MaterialContent(basePacks.occupiedModelData),
            GUIContent(),
            LanguageContent()
        )
        
        // Include asset packs
        assetPacks.forEach { pack ->
            LOGGER.info("Including asset pack ${pack.namespace}")
            copyBasicAssets(pack)
            contents.forEach { it.addFromPack(pack) }
        }
        
        // Write changes
        contents.forEach(PackContent::write)
        writeMetadata(assetPacks.size)
        
        // Create a zip
        return createZip()
    }
    
    private fun extractAssetPacks(): List<AssetPack> {
        return (AddonManager.loaders.asSequence().map { (id, loader) -> loader.file to id } + (NOVA.pluginFile to "nova"))
            .mapTo(ArrayList()) { (addonFile, namespace) ->
                val assetPackDir = File(ASSET_PACKS_DIR, namespace)
                
                val zip = ZipFile(addonFile)
                zip.fileHeaders.forEach { header ->
                    if (!header.isDirectory && header.fileName.startsWith("assets/")) {
                        val file = File(assetPackDir, header.fileName.substringAfter("assets/"))
                        val inputStream = zip.getInputStream(header)
                        if (header.fileName.endsWith(".png")) {
                            file.parentFile.mkdirs()
                            PNGMetadataRemover.remove(inputStream, file.outputStream())
                        } else file.write(inputStream)
                    }
                }
                
                return@mapTo AssetPack(assetPackDir, namespace)
            }
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
    
    private fun writeMetadata(packSize: Int) {
        val packMcmetaObj = JsonObject()
        val packObj = JsonObject().also { packMcmetaObj.add("pack", it) }
        packObj.addProperty("pack_format", 9)
        packObj.addProperty("description", "Nova ($packSize asset packs loaded)")
        
        PACK_MCMETA_FILE.parentFile.mkdirs()
        PACK_MCMETA_FILE.writeText(GSON.toJson(packMcmetaObj))
    }
    
    private fun createZip(): File {
        LOGGER.info("Packing zip")
        val parameters = ZipParameters().apply { isIncludeRootFolder = false }
        val zip = ZipFile(RESOURCE_PACK_FILE)
        zip.addFolder(PACK_DIR, parameters)
        IOUtils.removeZipTimestamps(RESOURCE_PACK_FILE)
        
        return RESOURCE_PACK_FILE
    }
    
}

internal enum class MaterialType(val material: Material) {
    
    DEFAULT(Material.SHULKER_SHELL),
    DAMAGEABLE(Material.FISHING_ROD),
    TRANSLUCENT(Material.SHULKER_SHELL),
    CONSUMABLE(Material.APPLE),
    ALWAYS_CONSUMABLE(Material.GOLDEN_APPLE),
    FAST_CONSUMABLE(Material.DRIED_KELP);
    
}
