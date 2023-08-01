package xyz.xenondevs.nova.data.resources.builder

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import com.google.gson.JsonObject
import kotlinx.coroutines.runBlocking
import xyz.xenondevs.commons.collections.enumMap
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.immutable.map
import xyz.xenondevs.commons.provider.immutable.mapNonNull
import xyz.xenondevs.commons.provider.immutable.orElse
import xyz.xenondevs.downloader.ExtractionMode
import xyz.xenondevs.downloader.MinecraftAssetsDownloader
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.addon.AddonManager
import xyz.xenondevs.nova.data.config.MAIN_CONFIG
import xyz.xenondevs.nova.data.config.PermanentStorage
import xyz.xenondevs.nova.data.config.configReloadable
import xyz.xenondevs.nova.data.config.entry
import xyz.xenondevs.nova.data.resources.builder.ResourceFilter.Type
import xyz.xenondevs.nova.data.resources.builder.basepack.BasePacks
import xyz.xenondevs.nova.data.resources.builder.task.AtlasContent
import xyz.xenondevs.nova.data.resources.builder.task.BarOverlayTask
import xyz.xenondevs.nova.data.resources.builder.task.BuildStage
import xyz.xenondevs.nova.data.resources.builder.task.EnchantmentContent
import xyz.xenondevs.nova.data.resources.builder.task.ExtractTask
import xyz.xenondevs.nova.data.resources.builder.task.LanguageContent
import xyz.xenondevs.nova.data.resources.builder.task.PackFunction
import xyz.xenondevs.nova.data.resources.builder.task.PackTaskHolder
import xyz.xenondevs.nova.data.resources.builder.task.armor.ArmorContent
import xyz.xenondevs.nova.data.resources.builder.task.font.FontContent
import xyz.xenondevs.nova.data.resources.builder.task.font.GuiContent
import xyz.xenondevs.nova.data.resources.builder.task.font.MoveCharactersContent
import xyz.xenondevs.nova.data.resources.builder.task.font.MovedFontContent
import xyz.xenondevs.nova.data.resources.builder.task.font.TextureIconContent
import xyz.xenondevs.nova.data.resources.builder.task.font.WailaContent
import xyz.xenondevs.nova.data.resources.builder.task.material.MaterialContent
import xyz.xenondevs.nova.data.serialization.json.GSON
import xyz.xenondevs.nova.ui.overlay.bossbar.BossBarOverlayManager
import xyz.xenondevs.nova.ui.waila.WailaManager
import xyz.xenondevs.nova.util.data.Version
import xyz.xenondevs.nova.util.runAsyncTask
import xyz.xenondevs.resourcepackobfuscator.ResourcePackObfuscator
import java.io.File
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.relativeTo
import kotlin.io.path.writeText
import kotlin.time.Duration
import kotlin.time.measureTime

private val EXTRACTION_MODE by MAIN_CONFIG.entry<String>("resource_pack", "generation", "minecraft_assets_source").map {
    when (it.lowercase()) {
        "github" -> ExtractionMode.GITHUB
        "mojang" -> ExtractionMode.MOJANG_ALL
        else -> throw IllegalArgumentException("Invalid minecraft_assets_source (must be \"github\" or \"mojang\")")
    }
}

private val CONFIG_RESOURCE_FILTERS by MAIN_CONFIG.entry<List<ResourceFilter>>("resource_pack", "generation", "resource_filters")
private val CORE_RESOURCE_FILTERS by configReloadable {
    buildList {
        this += ResourceFilter(ResourceFilter.Stage.ASSET_PACK, Type.BLACKLIST, Regex("^[a-z0-9._-]+/textures/armor/.*$"))
        
        if (!BossBarOverlayManager.ENABLED) {
            this += ResourceFilter(ResourceFilter.Stage.ASSET_PACK, Type.BLACKLIST, "minecraft/textures/gui/bars.png")
            this += ResourceFilter(ResourceFilter.Stage.ASSET_PACK, Type.BLACKLIST, "nova/font/bossbar*")
            this += ResourceFilter(ResourceFilter.Stage.ASSET_PACK, Type.BLACKLIST, "nova/textures/font/bars/*")
        }
        
        if (!WailaManager.ENABLED) {
            this += ResourceFilter(ResourceFilter.Stage.ASSET_PACK, Type.BLACKLIST, Regex("^[a-z0-9._-]+/textures/waila/.*$"))
            this += ResourceFilter(ResourceFilter.Stage.ASSET_PACK, Type.BLACKLIST, "nova/font/waila*")
            this += ResourceFilter(ResourceFilter.Stage.ASSET_PACK, Type.BLACKLIST, "nova/textures/font/waila/*")
        }
    }
}

private val OBFUSCATE by MAIN_CONFIG.entry<Boolean>("resource_pack", "generation", "protection", "obfuscate")
private val CORRUPT_ENTRIES by MAIN_CONFIG.entry<Boolean>("resource_pack", "generation", "protection", "corrupt_entries")
private val COMPRESSION_LEVEL by MAIN_CONFIG.entry<Int>("resource_pack", "generation", "compression_level")
private val PACK_DESCRIPTION by MAIN_CONFIG.entry<String>("resource_pack", "generation", "description")
private val IN_MEMORY_PROVIDER = MAIN_CONFIG.entry<Boolean>("resource_pack", "generation", "in_memory")
private val IN_MEMORY by IN_MEMORY_PROVIDER

private val SKIP_PACK_TASKS: Set<String> by MAIN_CONFIG.entry<HashSet<String>>("debug", "skip_pack_tasks")

class ResourcePackBuilder internal constructor() {
    
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
        private val RESOURCE_PACK_BUILD_DIR_PROVIDER: Provider<Path> = JIMFS_PROVIDER.mapNonNull { it.rootDirectories.first() }.orElse(RESOURCE_PACK_DIR.resolve(".build"))
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
        
        /**
         * A list of all [ResourceFilters][ResourceFilter] registered by addons.
         */
        private val customResourceFilters = ArrayList<ResourceFilter>()
        
        /**
         * A list of constructors for [PackTaskHolders][PackTaskHolder] that should be used to build the resource pack.
         */
        private val holderCreators: MutableList<(ResourcePackBuilder) -> PackTaskHolder> = mutableListOf(
            ::ExtractTask, ::MaterialContent, ::ArmorContent, ::GuiContent, ::LanguageContent, ::TextureIconContent,
            ::AtlasContent, ::WailaContent, ::MovedFontContent, ::CharSizeCalculator, ::SoundOverrides, ::FontContent,
            ::BarOverlayTask, ::MoveCharactersContent, ::EnchantmentContent
        )
        
        /**
         * Registers the specified [filters].
         */
        fun registerResourceFilters(vararg filters: ResourceFilter) {
            customResourceFilters += filters
        }
        
        /**
         * Registers specified [holderCreators].
         */
        fun registerTaskHolders(vararg holderCreators: (ResourcePackBuilder) -> PackTaskHolder) {
            this.holderCreators += holderCreators
        }
        
    }
    
    val basePacks = BasePacks(this)
    lateinit var assetPacks: List<AssetPack> private set
    
    @PublishedApi
    internal lateinit var holders: List<PackTaskHolder>
    private lateinit var tasksByStage: Map<BuildStage, List<PackFunction>>
    private val taskTimes = HashMap<PackFunction, Duration>()
    
    private val resourceFilters = sequenceOf(CONFIG_RESOURCE_FILTERS, CORE_RESOURCE_FILTERS, customResourceFilters)
        .flatten().groupByTo(enumMap()) { it.stage }
    
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
    
    internal fun buildPackCompletely() {
        LOGGER.info("Building resource pack")
        buildPackPreWorld()
        buildPackPostWorld()
    }
    
    internal fun buildPackPreWorld() {
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
            
            // sort and instantiate holders
            holders = holderCreators.map { it(this) }
            tasksByStage = PackFunction.getAndSortFunctions(holders).groupBy { it.stage }
            logTaskOrder()
            
            // load asset packs
            assetPacks = loadAssetPacks()
            LOGGER.info("Asset packs (${assetPacks.size}): ${assetPacks.joinToString(transform = AssetPack::namespace)}")
            
            // merge base packs
            basePacks.include()
            
            // run pack tasks
            LOGGER.info("Running pre-world pack tasks")
            tasksByStage[BuildStage.PRE_WORLD]?.forEach(::runPackFunction)
        } catch (t: Throwable) {
            // Only delete build dir in case of exception as building is continued in buildPostWorld()
            deleteBuildDir()
            throw t
        }
    }
    
    internal fun buildPackPostWorld() {
        try {
            // write post-world content
            LOGGER.info("Running post-world pack tasks")
            tasksByStage[BuildStage.POST_WORLD]?.forEach(::runPackFunction)
            
            // write metadata
            writeMetadata(assetPacks.size, basePacks.packAmount)
            
            // create zip
            createZip()
            LOGGER.info("ResourcePack created.")
            logTaskTimes()
            
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
    
    @Suppress("RemoveExplicitTypeArguments")
    private fun loadAssetPacks(): List<AssetPack> {
        return buildList<Triple<String, File, String>> {
            this += AddonManager.loaders.map { (id, loader) -> Triple(id, loader.file, "assets/") }
            this += Triple("nova", NOVA.novaJar, "assets/nova/")
        }.map { (namespace, file, assetsPath) ->
            val zip = FileSystems.newFileSystem(file.toPath())
            AssetPack(namespace, zip.getPath(assetsPath))
        }
    }
    
    private fun writeMetadata(assetPacks: Int, basePacks: Int) {
        val packMcmetaObj = JsonObject()
        val packObj = JsonObject().also { packMcmetaObj.add("pack", it) }
        packObj.addProperty("pack_format", 15)
        packObj.addProperty("description", PACK_DESCRIPTION.format(assetPacks, basePacks))
        
        PACK_MCMETA_FILE.parent.createDirectories()
        PACK_MCMETA_FILE.writeText(GSON.toJson(packMcmetaObj))
    }
    
    private fun createZip() {
        // delete old zip file
        RESOURCE_PACK_FILE.delete()
        
        // pack zip
        LOGGER.info("Packing zip...")
        val filters = getResourceFilters(ResourceFilter.Stage.RESOURCE_PACK)
        ResourcePackObfuscator(
            OBFUSCATE, CORRUPT_ENTRIES,
            PACK_DIR, RESOURCE_PACK_FILE.toPath(),
            MCASSETS_DIR.toFile()
        ) { file ->
            filters.all { filter -> filter.allows(file.relativeTo(ASSETS_DIR).invariantSeparatorsPathString) }
        }.packZip(COMPRESSION_LEVEL)
    }
    
    private fun runPackFunction(func: PackFunction) {
        if (func.toString() in SKIP_PACK_TASKS)
            return
        
        taskTimes[func] = measureTime { func.run() }
    }
    
    private fun logTaskOrder() {
        LOGGER.info("Tasks (${tasksByStage.values.sumOf(List<*>::size)}):")
        for ((stage, tasks) in tasksByStage) {
            LOGGER.info("  $stage (${tasks.size}):")
            for (task in tasks) {
                val skipped = task.toString() in SKIP_PACK_TASKS
                LOGGER.info("    $task" + if (skipped) " (skipped)" else "")
            }
        }
    }
    
    private fun logTaskTimes() {
        LOGGER.info("Time breakdown (Top 5):")
        taskTimes.entries.asSequence()
            .sortedByDescending { it.value }
            .take(5)
            .forEach { (task, time) -> LOGGER.info("  $task: $time") }
    }
    
    /**
     * Gets the resource filters for the specified [ResourceFilter.Stage].
     */
    fun getResourceFilters(stage: ResourceFilter.Stage): List<ResourceFilter> =
        resourceFilters[stage] ?: emptyList()
    
    /**
     * Retrieves the instantiated [PackTaskHolder] of the specified type.
     *
     * @throws IllegalArgumentException If no holder of the specified type is registered or hasn't been instantiated yet.
     */
    inline fun <reified T : PackTaskHolder> getHolder(): T {
        val holder = holders.firstOrNull { it is T }
            ?: throw IllegalArgumentException("No holder of type ${T::class.simpleName} is present")
        return holder as T
    }
    
    /**
     * Creates a [Lazy] that retrieves an instantiated [PackTaskHolder] of the specified type.
     */
    inline fun <reified T : PackTaskHolder> getHolderLazily(): Lazy<T> = lazy { getHolder<T>() }
    
}