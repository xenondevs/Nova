package xyz.xenondevs.nova.data.resources.builder

import com.google.gson.JsonObject
import kotlinx.coroutines.runBlocking
import net.lingala.zip4j.ZipFile
import xyz.xenondevs.downloader.ExtractionMode
import xyz.xenondevs.downloader.MinecraftAssetsDownloader
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.addon.AddonManager
import xyz.xenondevs.nova.data.config.DEFAULT_CONFIG
import xyz.xenondevs.nova.data.config.PermanentStorage
import xyz.xenondevs.nova.data.config.configReloadable
import xyz.xenondevs.nova.data.resources.builder.basepack.BasePacks
import xyz.xenondevs.nova.data.resources.builder.content.GUIContent
import xyz.xenondevs.nova.data.resources.builder.content.LanguageContent
import xyz.xenondevs.nova.data.resources.builder.content.material.MaterialContent
import xyz.xenondevs.nova.data.resources.builder.content.PackContent
import xyz.xenondevs.nova.data.resources.builder.content.TextureIconContent
import xyz.xenondevs.nova.data.resources.builder.content.WailaContent
import xyz.xenondevs.nova.ui.overlay.bossbar.BossBarOverlayManager
import xyz.xenondevs.nova.util.data.GSON
import xyz.xenondevs.nova.util.data.Version
import xyz.xenondevs.nova.util.data.write
import xyz.xenondevs.resourcepackobfuscator.ResourcePackObfuscator
import java.io.File

private val BOSS_BAR_ENABLED = { BossBarOverlayManager.isEnabled }
private val CORE_RESOURCE_FILTER = resourceFilterOf(
    "assets/minecraft/textures/gui/bars.png" to BOSS_BAR_ENABLED,
    "assets/nova/font/bossbar/*" to BOSS_BAR_ENABLED,
)
private val CONFIG_RESOURCE_FILTER by configReloadable { resourceFilterOf(*DEFAULT_CONFIG.getStringList("resource_pack.content_filters").toTypedArray()) }

private val OBFUSCATE by configReloadable { DEFAULT_CONFIG.getBoolean("resource_pack.protection.obfuscate") }
private val CORRUPT_ENTRIES by configReloadable { DEFAULT_CONFIG.getBoolean("resource_pack.protection.corrupt_entries") }

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
    
    private val resourceFilters = buildList {
        this += CORE_RESOURCE_FILTER
        this += CONFIG_RESOURCE_FILTER
        AddonManager.addons.values.forEach {
            val filter = it.resourceFilter ?: return@forEach
            this += filter
        }
    }
    
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
            // download minecraft assets if not present / outdated
            if (!MCASSETS_DIR.exists() || PermanentStorage.retrieveOrNull<Version>("minecraftAssetsVersion") != Version.SERVER_VERSION) {
                MCASSETS_DIR.deleteRecursively()
                runBlocking {
                    val downloader = MinecraftAssetsDownloader(
                        version = Version.SERVER_VERSION.toString(omitZeros = true),
                        outputDirectory = MCASSETS_DIR,
                        mode = ExtractionMode.GITHUB,
                        logger = LOGGER
                    )
                    downloader.downloadAssets()
                    PermanentStorage.store("minecraftAssetsVersion", Version.SERVER_VERSION)
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
            val zip = createZip()
            LOGGER.info("ResourcePack created.")
            return zip
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
                    try {
                        PNGMetadataRemover.remove(inputStream, file.outputStream())
                    } catch (e: IllegalStateException) {
                        LOGGER.warning("Failed to remove metadata from ${header.fileName}")
                        file.write(inputStream)
                    }
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
                            try {
                                PNGMetadataRemover.remove(inputStream, file.outputStream())
                            } catch (e: IllegalStateException) {
                                LOGGER.warning("Failed to remove metadata from ${file.name} in $namespace")
                                file.write(inputStream)
                            }
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
        resourceFilters.forEach(ResourceFilter::performFilterEvaluations)
        
        // filter files
        LOGGER.info("Applying resource filters")
        PACK_DIR.walkTopDown().forEach { file ->
            if (file.isDirectory)
                return@forEach
            
            val path = file.relativeTo(PACK_DIR).invariantSeparatorsPath
            if (resourceFilters.any { !it.test(path) })
                file.delete()
        }
        
        // delete old zip file
        RESOURCE_PACK_FILE.delete()
        
        // pack zip
        LOGGER.info("Packing zip...")
        ResourcePackObfuscator(
            OBFUSCATE, CORRUPT_ENTRIES,
            PACK_DIR, RESOURCE_PACK_FILE,
            MCASSETS_DIR
        ).packZip()
        
        return RESOURCE_PACK_FILE
    }
    
}