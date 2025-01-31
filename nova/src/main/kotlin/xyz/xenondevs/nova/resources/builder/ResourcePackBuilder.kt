package xyz.xenondevs.nova.resources.builder

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import com.google.gson.JsonObject
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import xyz.xenondevs.commons.collections.enumMap
import xyz.xenondevs.commons.provider.MutableProvider
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.combinedProvider
import xyz.xenondevs.commons.provider.flattenIterables
import xyz.xenondevs.commons.provider.map
import xyz.xenondevs.commons.provider.mapNonNull
import xyz.xenondevs.commons.provider.mutableProvider
import xyz.xenondevs.commons.provider.orElse
import xyz.xenondevs.downloader.ExtractionMode
import xyz.xenondevs.downloader.MinecraftAssetsDownloader
import xyz.xenondevs.nova.DATA_FOLDER
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA_JAR
import xyz.xenondevs.nova.PREVIOUS_NOVA_VERSION
import xyz.xenondevs.nova.addon.AddonBootstrapper
import xyz.xenondevs.nova.addon.id
import xyz.xenondevs.nova.config.MAIN_CONFIG
import xyz.xenondevs.nova.config.PermanentStorage
import xyz.xenondevs.nova.config.entry
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.ResourceType
import xyz.xenondevs.nova.resources.builder.ResourceFilter.Type
import xyz.xenondevs.nova.resources.builder.basepack.BasePacks
import xyz.xenondevs.nova.resources.builder.task.AtlasContent
import xyz.xenondevs.nova.resources.builder.task.BarOverlayTask
import xyz.xenondevs.nova.resources.builder.task.BuildStage
import xyz.xenondevs.nova.resources.builder.task.EquipmentContent
import xyz.xenondevs.nova.resources.builder.task.ExtractTask
import xyz.xenondevs.nova.resources.builder.task.LanguageContent
import xyz.xenondevs.nova.resources.builder.task.PackFunction
import xyz.xenondevs.nova.resources.builder.task.PackTaskHolder
import xyz.xenondevs.nova.resources.builder.task.TextureContent
import xyz.xenondevs.nova.resources.builder.task.TooltipStyleContent
import xyz.xenondevs.nova.resources.builder.task.font.FontContent
import xyz.xenondevs.nova.resources.builder.task.font.GuiContent
import xyz.xenondevs.nova.resources.builder.task.font.MoveCharactersContent
import xyz.xenondevs.nova.resources.builder.task.font.MovedFontContent
import xyz.xenondevs.nova.resources.builder.task.font.TextureIconContent
import xyz.xenondevs.nova.resources.builder.task.font.WailaContent
import xyz.xenondevs.nova.resources.builder.task.model.BlockModelContent
import xyz.xenondevs.nova.resources.builder.task.model.ItemModelContent
import xyz.xenondevs.nova.resources.builder.task.model.ModelContent
import xyz.xenondevs.nova.serialization.json.GSON
import xyz.xenondevs.nova.util.data.Version
import xyz.xenondevs.nova.util.data.readJson
import xyz.xenondevs.nova.util.data.writeImage
import xyz.xenondevs.nova.util.data.writeJson
import xyz.xenondevs.nova.util.runAsyncTask
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.isRegularFile
import kotlin.io.path.outputStream
import kotlin.io.path.relativeTo
import kotlin.io.path.walk
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
private val CORE_RESOURCE_FILTERS by combinedProvider(listOf(
    MAIN_CONFIG.entry<Boolean>("overlay", "bossbar", "enabled").map { enabled ->
        if (!enabled) {
            listOf(
                ResourceFilter(ResourceFilter.Stage.ASSET_PACK, Type.BLACKLIST, "minecraft/textures/gui/sprites/boss_bar/*"),
                ResourceFilter(ResourceFilter.Stage.ASSET_PACK, Type.BLACKLIST, "nova/font/bossbar*"),
                ResourceFilter(ResourceFilter.Stage.ASSET_PACK, Type.BLACKLIST, "nova/textures/font/bars/*")
            )
        } else emptyList()
    },
    MAIN_CONFIG.entry<Boolean>("waila", "enabled").map { enabled ->
        if (!enabled) {
            listOf(
                ResourceFilter(ResourceFilter.Stage.ASSET_PACK, Type.BLACKLIST, Regex("^[a-z0-9._-]+/textures/waila/.*$")),
                ResourceFilter(ResourceFilter.Stage.ASSET_PACK, Type.BLACKLIST, "nova/font/waila*"),
                ResourceFilter(ResourceFilter.Stage.ASSET_PACK, Type.BLACKLIST, "nova/textures/font/waila/*")
            )
        } else emptyList()
    }
)).flattenIterables()

private val COMPRESSION_LEVEL by MAIN_CONFIG.entry<Int>("resource_pack", "generation", "compression_level")
private val PACK_DESCRIPTION by MAIN_CONFIG.entry<String>("resource_pack", "generation", "description")
private val IN_MEMORY_PROVIDER = MAIN_CONFIG.entry<Boolean>("resource_pack", "generation", "in_memory")
private val IN_MEMORY by IN_MEMORY_PROVIDER

private val SKIP_PACK_TASKS: Set<String> by MAIN_CONFIG.entry<HashSet<String>>("debug", "skip_pack_tasks")

class ResourcePackBuilder internal constructor() {
    
    companion object {
        
        private val JIMFS_PROVIDER: MutableProvider<FileSystem> = mutableProvider { Jimfs.newFileSystem(Configuration.unix()) }
        
        //<editor-fold desc="never in memory">
        val RESOURCE_PACK_FILE: Path = DATA_FOLDER.resolve("resource_pack/ResourcePack.zip")
        val RESOURCE_PACK_DIR: Path = DATA_FOLDER.resolve("resource_pack")
        val BASE_PACKS_DIR: Path = RESOURCE_PACK_DIR.resolve("base_packs")
        val MCASSETS_DIR: Path = RESOURCE_PACK_DIR.resolve(".mcassets")
        val MCASSETS_ASSETS_DIR: Path = MCASSETS_DIR.resolve("assets")
        //</editor-fold>
        
        //<editor-fold desc="potentially in memory">
        private val RESOURCE_PACK_BUILD_DIR_PROVIDER: Provider<Path> = IN_MEMORY_PROVIDER.flatMap { inMemory -> 
            if (inMemory) 
                JIMFS_PROVIDER.map { it.rootDirectories.first() } 
            else provider(RESOURCE_PACK_DIR.resolve(".build"))
        }
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
            ::ExtractTask, ::EquipmentContent, ::GuiContent, ::LanguageContent, ::TextureIconContent,
            ::AtlasContent, ::WailaContent, ::MovedFontContent, ::CharSizeCalculator, ::SoundOverrides, ::FontContent,
            ::BarOverlayTask, ::MoveCharactersContent, ::ModelContent, ::BlockModelContent,
            ::ItemModelContent, ::TextureContent, ::TooltipStyleContent
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
    private var totalTime: Duration = Duration.ZERO
    
    private val resourceFilters = sequenceOf(CONFIG_RESOURCE_FILTERS, CORE_RESOURCE_FILTERS, customResourceFilters)
        .flatten().groupByTo(enumMap()) { it.stage }
    
    init {
        // delete legacy resource pack files
        File(DATA_FOLDER.toFile(), "ResourcePack").deleteRecursively()
        File(RESOURCE_PACK_DIR.toFile(), "asset_packs").deleteRecursively()
        File(RESOURCE_PACK_DIR.toFile(), "pack").deleteRecursively()
        if (!IN_MEMORY) RESOURCE_PACK_BUILD_DIR.toFile().deleteRecursively()
        
        if (PREVIOUS_NOVA_VERSION != null && PREVIOUS_NOVA_VERSION < Version("0.10")) {
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
            totalTime += measureTime {
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
                runBlocking { tasksByStage[BuildStage.PRE_WORLD]?.forEach { runPackFunction(it) } }
            }
        } catch (t: Throwable) {
            // Only delete build dir in case of exception as building is continued in buildPostWorld()
            deleteBuildDir()
            throw t
        }
    }
    
    internal fun buildPackPostWorld() {
        try {
            totalTime += measureTime {
                // write post-world content
                LOGGER.info("Running post-world pack tasks")
                runBlocking { tasksByStage[BuildStage.POST_WORLD]?.forEach { runPackFunction(it) } }
                
                // write metadata
                writeMetadata(assetPacks.size, basePacks.packAmount)
                
                // create zip
                createZip()
                logTaskTimes()
                
                // delete build dir asynchronously
                runAsyncTask { deleteBuildDir() }
            }
        } catch (t: Throwable) {
            // delete build dir
            deleteBuildDir()
            // re-throw t
            throw t
        }
    }
    
    private fun deleteBuildDir() {
        if (IN_MEMORY) {
            val jimfs = JIMFS_PROVIDER.get()
            jimfs.close()
            JIMFS_PROVIDER.set(Jimfs.newFileSystem(Configuration.unix()))
        } else {
            RESOURCE_PACK_BUILD_DIR.toFile().deleteRecursively()
        }
    }
    
    @Suppress("RemoveExplicitTypeArguments")
    private fun loadAssetPacks(): List<AssetPack> {
        return buildList<Triple<String, Path, String>> {
            this += AddonBootstrapper.addons.map { addon -> Triple(addon.id, addon.file, "assets/") }
            this += Triple("nova", NOVA_JAR, "assets/nova/")
        }.map { (namespace, file, assetsPath) ->
            val zip = FileSystems.newFileSystem(file)
            AssetPack(namespace, zip.getPath(assetsPath))
        }
    }
    
    private fun writeMetadata(assetPacks: Int, basePacks: Int) {
        val packMcmetaObj = JsonObject()
        val packObj = JsonObject().also { packMcmetaObj.add("pack", it) }
        packObj.addProperty("pack_format", 15)
        val supportedFormats = JsonObject().also { packObj.add("supported_formats", it) }
        supportedFormats.addProperty("min_inclusive", 0)
        supportedFormats.addProperty("max_inclusive", 999)
        packObj.addProperty("description", PACK_DESCRIPTION.format(assetPacks, basePacks))
        
        PACK_MCMETA_FILE.parent.createDirectories()
        PACK_MCMETA_FILE.writeText(GSON.toJson(packMcmetaObj))
    }
    
    private fun createZip() {
        // delete old zip file
        RESOURCE_PACK_FILE.deleteIfExists()
        
        // pack zip
        LOGGER.info("Packing zip...")
        val filters = getResourceFilters(ResourceFilter.Stage.RESOURCE_PACK)
        ZipOutputStream(RESOURCE_PACK_FILE.outputStream()).use { zip ->
            zip.setLevel(COMPRESSION_LEVEL)
            PACK_DIR.walk()
                .filter { path -> path.isRegularFile() }
                .filter { path -> filters.all { filter -> filter.allows(path.relativeTo(ASSETS_DIR).invariantSeparatorsPathString) } }
                .forEach { path ->
                    zip.putNextEntry(ZipEntry(path.relativeTo(PACK_DIR).invariantSeparatorsPathString))
                    path.inputStream().use { it.copyTo(zip) }
                }
        }
    }
    
    private suspend fun runPackFunction(func: PackFunction) {
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
        LOGGER.info("Resource pack built in ${totalTime}:")
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
    
    /**
     * Searches for a file under [path] in both the resource pack and vanilla minecraft assets,
     * returning the [Path] if it exists or `null` if it doesn't.
     */
    fun findOrNull(path: ResourcePath<*>): Path? =
        resolve(path).takeIf(Path::exists) ?: resolveVanilla(path).takeIf(Path::exists)
    
    /**
     * Searches for a file under [path] in both the resource pack and vanilla minecraft assets,
     * returning the [Path] if it exists or throwing an [IllegalArgumentException] if it doesn't.
     */
    fun findOrThrow(path: ResourcePath<*>): Path =
        findOrNull(path) ?: throw IllegalArgumentException("Resource not found: ${path.filePath}")
    
    /**
     * Resolves the file under [path] in the vanilla minecraft assets.
     */
    fun resolveVanilla(path: ResourcePath<*>): Path =
        MCASSETS_DIR.resolve(path.filePath)
    
    /**
     * Resolves the corresponding `.mcmeta` file for the specified [path] in the vanilla minecraft assets.
     */
    fun resolveVanillaMeta(path: ResourcePath<ResourceType.HasMcMeta>): Path =
        MCASSETS_DIR.resolve("${path.filePath}.mcmeta")
    
    /**
     * Resolves the file under [path] in the vanilla minecraft assets.
     */
    fun resolveVanilla(path: String): Path =
        MCASSETS_DIR.resolve(path)
    
    /**
     * Resolves the file under [path] in the resource pack.
     */
    fun resolve(path: ResourcePath<*>): Path =
        PACK_DIR.resolve(path.filePath)
    
    /**
     * Resolves the corresponding `.mcmeta` file for the specified [path].
     */
    fun resolveMeta(path: ResourcePath<ResourceType.HasMcMeta>): Path =
        PACK_DIR.resolve("${path.filePath}.mcmeta")
    
    /**
     * Resolves the file under [path] in the resource pack.
     *
     * Example paths: `pack.json`, `assets/minecraft/textures/block/dirt.png`
     */
    fun resolve(path: String): Path =
        PACK_DIR.resolve(path)
    
    /**
     * Deserializes the JSON content of the file under [path] in the resource pack
     * to [V] using [json], or returns `null` if the file does not exist.
     */
    inline fun <reified V> readJson(path: ResourcePath<ResourceType.JsonFile>, json: Json = Json): V? {
        val file = resolve(path)
        return if (file.exists()) file.readJson(json) else null
    }
    
    /**
     * Serializes [value] to JSON using [json] and writes it to the file
     * under [path], creating parent directories if necessary.
     */
    inline fun <reified V> writeJson(path: ResourcePath<ResourceType.JsonFile>, value: V, json: Json = Json) {
        val file = resolve(path)
        file.parent.createDirectories()
        file.writeJson(value, json)
    }
    
    /**
     * Serializes [value] to JSON using [json] and writes it to the
     * corresponding `.mcmeta` file for the specified [path], creating parent directories if necessary.
     */
    inline fun <reified V> writeMeta(path: ResourcePath<ResourceType.HasMcMeta>, value: V, json: Json = Json) {
        val file = resolveMeta(path)
        file.parent.createDirectories()
        file.writeJson(value, json)
    }
    
    /**
     * Writes the [image] to the file under [path], creating parent directories if necessary.
     */
    fun writeImage(path: ResourcePath<ResourceType.PngFile>, image: BufferedImage) {
        val file = resolve(path)
        file.parent.createDirectories()
        file.writeImage(image, "PNG")
    }
    
}