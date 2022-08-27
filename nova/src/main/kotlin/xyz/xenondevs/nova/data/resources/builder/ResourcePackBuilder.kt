package xyz.xenondevs.nova.data.resources.builder

import com.google.gson.JsonObject
import kotlinx.coroutines.runBlocking
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import xyz.xenondevs.downloader.ExtractionMode
import xyz.xenondevs.downloader.MinecraftAssetsDownloader
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.addon.AddonManager
import xyz.xenondevs.nova.addon.assets.AssetPack
import xyz.xenondevs.nova.data.resources.builder.basepack.BasePacks
import xyz.xenondevs.nova.data.resources.builder.content.GUIContent
import xyz.xenondevs.nova.data.resources.builder.content.LanguageContent
import xyz.xenondevs.nova.data.resources.builder.content.MaterialContent
import xyz.xenondevs.nova.data.resources.builder.content.PackContent
import xyz.xenondevs.nova.data.resources.builder.content.TextureIconContent
import xyz.xenondevs.nova.data.resources.builder.content.WailaContent
import xyz.xenondevs.nova.util.data.GSON
import xyz.xenondevs.nova.util.data.Version
import xyz.xenondevs.nova.util.data.write
import java.io.File
import java.util.function.Predicate

@Suppress("MemberVisibilityCanBePrivate")
internal object ResourcePackBuilder {
    
    val RESOURCE_PACK_DIR = File(NOVA.dataFolder, "resource_pack")
    val RESOURCE_PACK_BUILD_DIR = File(RESOURCE_PACK_DIR, ".build")
    val BASE_PACKS_DIR = File(RESOURCE_PACK_DIR, "base_packs")
    val TEMP_BASE_PACKS_DIR = File(RESOURCE_PACK_BUILD_DIR, "base_packs")
    val ASSET_PACKS_DIR = File(RESOURCE_PACK_BUILD_DIR, "asset_packs")
    val PACK_DIR = File(RESOURCE_PACK_BUILD_DIR, "pack")
    val ASSETS_DIR = File(PACK_DIR, "assets")
    val MINECRAFT_ASSETS_DIR = File(ASSETS_DIR, "minecraft")
    val LANGUAGE_DIR = File(ASSETS_DIR, "minecraft/lang")
    val FONT_DIR = File(ASSETS_DIR, "nova/font")
    val GUIS_FILE = File(FONT_DIR, "gui.json")
    val PACK_MCMETA_FILE = File(PACK_DIR, "pack.mcmeta")
    val RESOURCE_PACK_FILE = File(RESOURCE_PACK_DIR, "ResourcePack.zip")
    val MCASSETS_DIR = File(RESOURCE_PACK_DIR, ".mcassets")
    
    init {
        // delete legacy resource pack files
        File(NOVA.dataFolder, "ResourcePack").deleteRecursively()
        File(RESOURCE_PACK_DIR, "asset_packs").deleteRecursively()
        File(RESOURCE_PACK_DIR, "pack").deleteRecursively()
        RESOURCE_PACK_BUILD_DIR.deleteRecursively()
        
        if (NOVA.lastVersion != null && NOVA.lastVersion!! < Version("0.10")) {
            BASE_PACKS_DIR.delete()
        }
        
        // create base packs folder
        BASE_PACKS_DIR.mkdirs()
    }
    
    fun buildPack(): File {
        LOGGER.info("Building resource pack")
        
        try {
            // download minecraft assets if not present
            if (!MCASSETS_DIR.exists()) {
                LOGGER.info("Downloading minecraft assets")
                runBlocking {
                    val downloader = MinecraftAssetsDownloader(outputDirectory = MCASSETS_DIR, mode = ExtractionMode.ALL)
                    downloader.filters += Predicate { it.startsWith("assets/minecraft/textures/") || it.startsWith("assets/minecraft/lang/") }
                    downloader.downloadAssets()
                }
            }
            
            // extract files
            val basePacks = BasePacks().also(BasePacks::include)
            val assetPacks = extractAssetPacks()
            
            // extract assets/minecraft
            extractMinecraftAssets()
            
            // init content
            val contents = listOf(
                MaterialContent(basePacks),
                GUIContent(),
                LanguageContent(),
                WailaContent(),
                TextureIconContent()
            )
            
            // Include asset packs
            assetPacks.forEach { pack ->
                LOGGER.info("Including asset pack ${pack.namespace}")
                copyBasicAssets(pack)
                contents.forEach { it.addFromPack(pack) }
            }
            
            // Write changes
            LOGGER.info("Writing content")
            contents.forEach(PackContent::write)
            writeMetadata(assetPacks.size, basePacks.packAmount)
            
            // Create a zip
            return createZip()
        } finally {
            RESOURCE_PACK_BUILD_DIR.deleteRecursively()
        }
    }
    
    private fun extractMinecraftAssets() {
        val zip = ZipFile(NOVA.pluginFile)
        zip.fileHeaders.forEach { header ->
            if (!header.isDirectory && header.fileName.startsWith("assets/minecraft/")) {
                val file = File(MINECRAFT_ASSETS_DIR, header.fileName.substringAfter("assets/minecraft/"))
                val inputStream = zip.getInputStream(header)
                file.parentFile.mkdirs()
                if (header.fileName.endsWith(".png")) {
                    PNGMetadataRemover.remove(inputStream, file.outputStream())
                } else file.write(inputStream)
            }
        }
    }
    
    private fun extractAssetPacks(): List<AssetPack> {
        return (AddonManager.loaders.asSequence().map { (id, loader) -> Triple(loader.file, id, "assets/") }
            + Triple(NOVA.pluginFile, "nova", "assets/nova/")
            ).mapTo(ArrayList()) { (addonFile, namespace, assetsDirPath) ->
                val assetPackDir = File(ASSET_PACKS_DIR, namespace)
                
                val zip = ZipFile(addonFile)
                zip.fileHeaders.forEach { header ->
                    if (!header.isDirectory && header.fileName.startsWith(assetsDirPath)) {
                        val file = File(assetPackDir, header.fileName.substringAfter(assetsDirPath))
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
    
    private fun writeMetadata(assetPacks: Int, basePacks: Int) {
        val packMcmetaObj = JsonObject()
        val packObj = JsonObject().also { packMcmetaObj.add("pack", it) }
        packObj.addProperty("pack_format", 9)
        packObj.addProperty("description", "Nova ($assetPacks asset pack(s), $basePacks base pack(s))")
        
        PACK_MCMETA_FILE.parentFile.mkdirs()
        PACK_MCMETA_FILE.writeText(GSON.toJson(packMcmetaObj))
    }
    
    private fun createZip(): File {
        LOGGER.info("Packing zip")
        
        // delete old zip file
        RESOURCE_PACK_FILE.delete()
        
        // pack zip
        val parameters = ZipParameters().apply {
            isIncludeRootFolder = false
            lastModifiedFileTime = 1
        }
        val zip = ZipFile(RESOURCE_PACK_FILE)
        zip.addFolder(PACK_DIR, parameters)
        
        return RESOURCE_PACK_FILE
    }
    
}