package xyz.xenondevs.nova.data.resources.builder

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import com.google.gson.JsonObject
import kotlinx.coroutines.runBlocking
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.immutable.combinedProvider
import xyz.xenondevs.commons.provider.immutable.flatten
import xyz.xenondevs.commons.provider.immutable.map
import xyz.xenondevs.commons.provider.immutable.orElse
import xyz.xenondevs.downloader.ExtractionMode
import xyz.xenondevs.downloader.MinecraftAssetsDownloader
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.addon.AddonManager
import xyz.xenondevs.nova.data.config.DEFAULT_CONFIG
import xyz.xenondevs.nova.data.config.PermanentStorage
import xyz.xenondevs.nova.data.config.configReloadable
import xyz.xenondevs.nova.data.resources.CharSizes
import xyz.xenondevs.nova.data.resources.ResourcePath
import xyz.xenondevs.nova.data.resources.builder.ResourceFilter.Stage
import xyz.xenondevs.nova.data.resources.builder.ResourceFilter.Type
import xyz.xenondevs.nova.data.resources.builder.basepack.BasePacks
import xyz.xenondevs.nova.data.resources.builder.content.AtlasContent
import xyz.xenondevs.nova.data.resources.builder.content.LanguageContent
import xyz.xenondevs.nova.data.resources.builder.content.PackContent
import xyz.xenondevs.nova.data.resources.builder.content.armor.ArmorContent
import xyz.xenondevs.nova.data.resources.builder.content.font.GuiContent
import xyz.xenondevs.nova.data.resources.builder.content.font.MovedFontContent
import xyz.xenondevs.nova.data.resources.builder.content.font.TextureIconContent
import xyz.xenondevs.nova.data.resources.builder.content.font.WailaContent
import xyz.xenondevs.nova.data.resources.builder.content.material.MaterialContent
import xyz.xenondevs.nova.data.serialization.json.GSON
import xyz.xenondevs.nova.util.data.Version
import xyz.xenondevs.nova.util.data.getConfigurationSectionList
import xyz.xenondevs.nova.util.data.openZip
import xyz.xenondevs.nova.util.runAsyncTask
import xyz.xenondevs.resourcepackobfuscator.ResourcePackObfuscator
import java.io.File
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Path
import kotlin.io.path.CopyActionResult
import kotlin.io.path.copyToRecursively
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.inputStream
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.isDirectory
import kotlin.io.path.outputStream
import kotlin.io.path.relativeTo
import kotlin.io.path.writeText


private val EXTRACTION_MODE by configReloadable {
    when (DEFAULT_CONFIG.getString("resource_pack.generation.minecraft_assets_source")!!.lowercase()) {
        "github" -> ExtractionMode.GITHUB
        "mojang" -> ExtractionMode.MOJANG_ALL
        else -> throw IllegalArgumentException("Invalid minecraft_assets_source (must be \"github\" or \"mojang\")")
    }
}

private val CONFIG_RESOURCE_FILTERS = configReloadable { DEFAULT_CONFIG.getConfigurationSectionList("resource_pack.generation.resource_filters").map(ResourceFilter::of) }
private val CORE_RESOURCE_FILTERS = configReloadable {
    buildList {
        if (!DEFAULT_CONFIG.getBoolean("overlay.bossbar.enabled")) {
            this += ResourceFilter(Stage.ASSET_PACK, Type.BLACKLIST, "minecraft/textures/gui/bars.png")
            this += ResourceFilter(Stage.ASSET_PACK, Type.BLACKLIST, "nova/font/bossbar/*")
            this += ResourceFilter(Stage.ASSET_PACK, Type.BLACKLIST, "nova/textures/font/bars/*")
        }
        if (!DEFAULT_CONFIG.getBoolean("debug.hide_empty_tooltip")) {
            this += ResourceFilter(Stage.ASSET_PACK, Type.BLACKLIST, "minecraft/shaders/core/position_color*")
        }
    }
}

private val OBFUSCATE by configReloadable { DEFAULT_CONFIG.getBoolean("resource_pack.generation.protection.obfuscate") }
private val CORRUPT_ENTRIES by configReloadable { DEFAULT_CONFIG.getBoolean("resource_pack.generation.protection.corrupt_entries") }
private val COMPRESSION_LEVEL by configReloadable { DEFAULT_CONFIG.getInt("resource_pack.generation.compression_level") }
private val PACK_DESCRIPTION by configReloadable { DEFAULT_CONFIG.getString("resource_pack.generation.description")!! }
private val IN_MEMORY_PROVIDER = configReloadable { DEFAULT_CONFIG.getBoolean("resource_pack.generation.in_memory") }
private val IN_MEMORY by IN_MEMORY_PROVIDER

@Suppress("MemberVisibilityCanBePrivate")
internal class ResourcePackBuilder {
    
    companion object {
        
        private var JIMFS_PROVIDER: Provider<FileSystem?> = IN_MEMORY_PROVIDER.map { if (it) Jimfs.newFileSystem(Configuration.unix()) else null }
        
        //<editor-fold desc="never in memory">
        val RESOURCE_PACK_FILE: File = File(NOVA.dataFolder, "resource_pack/ResourcePack.zip")
        val RESOURCE_PACK_DIR: Path = File(NOVA.dataFolder, "resource_pack").toPath()
        val BASE_PACKS_DIR: Path = RESOURCE_PACK_DIR.resolve("base_packs")
        val MCASSETS_DIR: Path = RESOURCE_PACK_DIR.resolve(".mcassets")
        val MCASSETS_ASSETS_DIR: Path = MCASSETS_DIR.resolve("assets")
        //</editor-fold>
        
        //<editor-fold desc="potentially in memory">
        private val RESOURCE_PACK_BUILD_DIR_PROVIDER: Provider<Path> = JIMFS_PROVIDER.map { it.rootDirectories.first() }.orElse(RESOURCE_PACK_DIR.resolve(".build"))
        private val TEMP_BASE_PACKS_DIR_PROVIDER: Provider<Path> = RESOURCE_PACK_BUILD_DIR_PROVIDER.map { it.resolve("base_packs") }
        private val PACK_DIR_PROVIDER: Provider<Path> = RESOURCE_PACK_BUILD_DIR_PROVIDER.map { it.resolve("pack") }
        private val ASSETS_DIR_PROVIDER: Provider<Path> = PACK_DIR_PROVIDER.map { it.resolve("assets") }
        private val MINECRAFT_ASSETS_DIR_PROVIDER: Provider<Path> = ASSETS_DIR_PROVIDER.map { it.resolve("minecraft") }
        private val LANGUAGE_DIR_PROVIDER: Provider<Path> = MINECRAFT_ASSETS_DIR_PROVIDER.map { it.resolve("lang") }
        private val FONT_DIR_PROVIDER: Provider<Path> = ASSETS_DIR_PROVIDER.map { it.resolve("nova/font") }
        private val PACK_MCMETA_FILE_PROVIDER: Provider<Path> = PACK_DIR_PROVIDER.map { it.resolve("pack.mcmeta") }
        
        val RESOURCE_PACK_BUILD_DIR: Path by RESOURCE_PACK_BUILD_DIR_PROVIDER
        val TEMP_BASE_PACKS_DIR: Path by TEMP_BASE_PACKS_DIR_PROVIDER
        val PACK_DIR: Path by PACK_DIR_PROVIDER
        val ASSETS_DIR: Path by ASSETS_DIR_PROVIDER
        val MINECRAFT_ASSETS_DIR: Path by MINECRAFT_ASSETS_DIR_PROVIDER
        val LANGUAGE_DIR: Path by LANGUAGE_DIR_PROVIDER
        val FONT_DIR: Path by FONT_DIR_PROVIDER
        val PACK_MCMETA_FILE: Path by PACK_MCMETA_FILE_PROVIDER
        //</editor-fold>
        
        private val RESOURCE_FILTERS: Map<Stage, List<ResourceFilter>> by combinedProvider(listOf(CONFIG_RESOURCE_FILTERS, CORE_RESOURCE_FILTERS))
            .flatten()
            .map { filters -> filters.groupBy { filter -> filter.stage } }
        
    }
    
    val soundOverrides = SoundOverrides()
    val movedFonts = MovedFontContent()
    val basePacks = BasePacks(this)
    
    private lateinit var assetPacks: List<AssetPack>
    private lateinit var contents: List<PackContent>
    
    init {
        // delete legacy resource pack files
        File(NOVA.dataFolder, "ResourcePack").deleteRecursively()
        File(RESOURCE_PACK_DIR.toFile(), "asset_packs").deleteRecursively()
        File(RESOURCE_PACK_DIR.toFile(), "pack").deleteRecursively()
        if (!IN_MEMORY) RESOURCE_PACK_BUILD_DIR.toFile().deleteRecursively()
        
        if (NOVA.lastVersion != null && NOVA.lastVersion!! < Version("0.10")) {
            BASE_PACKS_DIR.toFile().delete()
        }
        
        // create base packs folder
        BASE_PACKS_DIR.createDirectories()
    }
    
    fun buildPackCompletely() {
        LOGGER.info("Building resource pack")
        buildPackPreWorld()
        buildPackPostWorld()
    }
    
    fun buildPackPreWorld() {
        try {
            // download minecraft assets if not present / outdated
            if (!MCASSETS_DIR.exists() || PermanentStorage.retrieveOrNull<Version>("minecraftAssetsVersion") != Version.SERVER_VERSION) {
                MCASSETS_DIR.toFile().deleteRecursively()
                runBlocking {
                    val downloader = MinecraftAssetsDownloader(
                        version = Version.SERVER_VERSION.toString(omitZeros = true),
                        outputDirectory = MCASSETS_DIR.toFile(),
                        mode = EXTRACTION_MODE,
                        logger = LOGGER
                    )
                    try {
                        downloader.downloadAssets()
                    } catch (ex: Exception) {
                        throw IllegalStateException(buildString {
                            append("Failed to download minecraft assets. Check your firewall settings.")
                            if (EXTRACTION_MODE == ExtractionMode.GITHUB)
                                append(" If your server can't access github.com in general, you can change \"minecraft_assets_source\" in the config to \"mojang\".")
                        }, ex)
                    }
                    PermanentStorage.store("minecraftAssetsVersion", Version.SERVER_VERSION)
                }
            }
            
            // load base- and asset packs
            basePacks.include()
            assetPacks = loadAssetPacks()
            LOGGER.info("Asset packs (${assetPacks.size}): ${assetPacks.joinToString(transform = AssetPack::namespace)}")
            
            // init content
            contents = listOf(
                // pre-world
                MaterialContent(this),
                ArmorContent(this),
                GuiContent(),
                LanguageContent(),
                TextureIconContent(this),
                AtlasContent(),
                
                // post-world
                WailaContent(this),
                movedFonts
            )
            
            // init pre-world content
            val preWorldContents = contents.filter { it.stage == BuildingStage.PRE_WORLD }
            preWorldContents.forEach(PackContent::init)
            
            // extract resources
            LOGGER.info("Extracting resources")
            extractMinecraftAssets()
            assetPacks.forEach(::extractResources)
            
            // write pre-world content
            LOGGER.info("Writing pre-world content")
            assetPacks.forEach { pack -> preWorldContents.forEach { it.includePack(pack) } }
            preWorldContents.forEach(PackContent::write)
            
            writeMetadata(assetPacks.size, basePacks.packAmount)
        } catch (t: Throwable) {
            // Only delete build dir in case of exception as building is continued in buildPostWorld()
            deleteBuildDir()
            throw t
        }
    }
    
    fun buildPackPostWorld() {
        try {
            // write post-world content
            LOGGER.info("Writing post-world content")
            val postWorldContents = contents.filter { it.stage == BuildingStage.POST_WORLD }
            postWorldContents.forEach(PackContent::init)
            assetPacks.forEach { pack -> postWorldContents.forEach { it.includePack(pack) } }
            postWorldContents.forEach(PackContent::write)
            
            // write sound overrides
            LOGGER.info("Writing sound overrides")
            soundOverrides.write()
            
            // calculate char sizes
            LOGGER.info("Calculating char sizes")
            CharSizeCalculator().calculateCharSizes()
            CharSizes.invalidateCache()
            
            // write metadata
            writeMetadata(assetPacks.size, basePacks.packAmount)
            
            // create zip
            createZip()
            LOGGER.info("ResourcePack created.")
            
            // delete build dir asynchronously
            runAsyncTask { deleteBuildDir() }
        } catch (t: Throwable) {
            // delete build dir
            deleteBuildDir()
            // re-throw t
            throw t
        }
    }
    
    private fun deleteBuildDir() {
        val provider = JIMFS_PROVIDER.value
        if (provider != null) {
            provider.close()
            JIMFS_PROVIDER.update() // creates a new jimfs file system
        } else {
            RESOURCE_PACK_BUILD_DIR.toFile().deleteRecursively()
        }
    }
    
    private fun extractMinecraftAssets() {
        val zip = NOVA.pluginFile.openZip()
        zip.resolve("assets/minecraft/")
            .copyToRecursively(
                MINECRAFT_ASSETS_DIR,
                followLinks = false,
            ) { source, target ->
                if (source.isDirectory())
                    return@copyToRecursively CopyActionResult.CONTINUE
                
                val relPath = target.relativeTo(MINECRAFT_ASSETS_DIR)
                
                if (RESOURCE_FILTERS[Stage.ASSET_PACK]?.all { filter -> filter.allows("minecraft/$relPath") } == false)
                    return@copyToRecursively CopyActionResult.SKIP_SUBTREE
                
                source.inputStream().use { ins ->
                    target.parent.createDirectories()
                    target.outputStream().use { out ->
                        if (source.extension.equals("png", true))
                            PNGMetadataRemover.remove(ins, out)
                        else ins.transferTo(out)
                    }
                }
                
                CopyActionResult.CONTINUE
            }
    }
    
    @Suppress("RemoveExplicitTypeArguments")
    private fun loadAssetPacks(): List<AssetPack> {
        return buildList<Triple<String, File, String>> {
            this += AddonManager.loaders.map { (id, loader) -> Triple(id, loader.file, "assets/") }
            this += Triple("nova", NOVA.pluginFile, "assets/nova/")
        }.map { (namespace, file, assetsPath) ->
            val zip = FileSystems.newFileSystem(file.toPath())
            AssetPack(namespace, zip.getPath(assetsPath))
        }
    }
    
    private fun extractResources(pack: AssetPack) {
        val namespace = pack.namespace
        pack.extract(
            ASSETS_DIR.resolve(namespace)
        ) { relPath ->
            contents.none { it.excludesPath(ResourcePath(namespace, relPath)) }
                && RESOURCE_FILTERS[Stage.ASSET_PACK]?.all { filter -> filter.allows("$namespace/$relPath") } ?: true
        }
    }
    
    private fun writeMetadata(assetPacks: Int, basePacks: Int) {
        val packMcmetaObj = JsonObject()
        val packObj = JsonObject().also { packMcmetaObj.add("pack", it) }
        packObj.addProperty("pack_format", 13)
        packObj.addProperty("description", PACK_DESCRIPTION.format(assetPacks, basePacks))
        
        PACK_MCMETA_FILE.parent.createDirectories()
        PACK_MCMETA_FILE.writeText(GSON.toJson(packMcmetaObj))
    }
    
    private fun createZip() {
        // delete old zip file
        RESOURCE_PACK_FILE.delete()
        
        // pack zip
        LOGGER.info("Packing zip...")
        ResourcePackObfuscator(
            OBFUSCATE, CORRUPT_ENTRIES,
            PACK_DIR, RESOURCE_PACK_FILE.toPath(),
            MCASSETS_DIR.toFile()
        ) { file ->
            RESOURCE_FILTERS[Stage.RESOURCE_PACK]
                ?.all { filter -> filter.allows(file.relativeTo(ASSETS_DIR).invariantSeparatorsPathString) }
                ?: true
        }.packZip(COMPRESSION_LEVEL)
    }
    
    enum class BuildingStage {
        PRE_WORLD,
        POST_WORLD
    }
    
}