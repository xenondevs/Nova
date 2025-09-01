package xyz.xenondevs.nova.resources.builder

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.key.Key
import net.minecraft.SharedConstants
import net.minecraft.server.packs.PackType
import org.slf4j.Logger
import xyz.xenondevs.commons.collections.enumMap
import xyz.xenondevs.commons.provider.combinedProvider
import xyz.xenondevs.commons.provider.flattenIterables
import xyz.xenondevs.commons.reflection.simpleNestedName
import xyz.xenondevs.commons.version.Version
import xyz.xenondevs.downloader.ExtractionMode
import xyz.xenondevs.downloader.MinecraftAssetsDownloader
import xyz.xenondevs.nova.DATA_FOLDER
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA_JAR
import xyz.xenondevs.nova.addon.AddonBootstrapper
import xyz.xenondevs.nova.addon.id
import xyz.xenondevs.nova.config.MAIN_CONFIG
import xyz.xenondevs.nova.config.PermanentStorage
import xyz.xenondevs.nova.config.entry
import xyz.xenondevs.nova.resources.AutoCopier
import xyz.xenondevs.nova.resources.ResourcePackManager
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.ResourceType
import xyz.xenondevs.nova.resources.builder.ResourceFilter.Type
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder.Companion.configure
import xyz.xenondevs.nova.resources.builder.task.AtlasTask
import xyz.xenondevs.nova.resources.builder.task.BlockModelTask
import xyz.xenondevs.nova.resources.builder.task.BlockStateContent
import xyz.xenondevs.nova.resources.builder.task.BossBarOverlayTask
import xyz.xenondevs.nova.resources.builder.task.BuildStage
import xyz.xenondevs.nova.resources.builder.task.CharSizeCalculator
import xyz.xenondevs.nova.resources.builder.task.EntityVariantTask
import xyz.xenondevs.nova.resources.builder.task.EquipmentTask
import xyz.xenondevs.nova.resources.builder.task.ExtractTask
import xyz.xenondevs.nova.resources.builder.task.FontContent
import xyz.xenondevs.nova.resources.builder.task.GuiTextureTask
import xyz.xenondevs.nova.resources.builder.task.ItemModelContent
import xyz.xenondevs.nova.resources.builder.task.LanguageContent
import xyz.xenondevs.nova.resources.builder.task.ModelContent
import xyz.xenondevs.nova.resources.builder.task.MoveCharactersTask
import xyz.xenondevs.nova.resources.builder.task.MovedFontContent
import xyz.xenondevs.nova.resources.builder.task.PackBuildData
import xyz.xenondevs.nova.resources.builder.task.PackMcMetaTask
import xyz.xenondevs.nova.resources.builder.task.PackTask
import xyz.xenondevs.nova.resources.builder.task.SoundOverridesTask
import xyz.xenondevs.nova.resources.builder.task.TextureContent
import xyz.xenondevs.nova.resources.builder.task.TextureIconContent
import xyz.xenondevs.nova.resources.builder.task.TooltipStyleContent
import xyz.xenondevs.nova.resources.builder.task.WailaTask
import xyz.xenondevs.nova.resources.builder.task.basepack.BasePacks
import xyz.xenondevs.nova.resources.upload.AutoUploadManager
import xyz.xenondevs.nova.util.SERVER_VERSION
import xyz.xenondevs.nova.util.data.readJson
import xyz.xenondevs.nova.util.data.writeImage
import xyz.xenondevs.nova.util.data.writeJson
import java.awt.image.BufferedImage
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
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

private val SKIP_PACK_TASKS: Set<String> by MAIN_CONFIG.entry<HashSet<String>>("debug", "skip_pack_tasks")

/**
 * Builds a resource pack based on a [ResourcePackConfiguration].
 * A [ResourcePackBuilder] is responsible for a single resource pack build session.
 */
class ResourcePackBuilder internal constructor(
    /**
     * The id of the resource pack.
     */
    val id: Key,
    /**
     * The logger for this resource pack build session.
     */
    val logger: Logger,
) {
    
    companion object {
        
        /**
         * The resource pack format version of the current Minecraft version.
         */
        val PACK_VERSION: Int = SharedConstants.getCurrentVersion().packVersion(PackType.CLIENT_RESOURCES)
        
        /**
         * The id of the core resource pack (``nova:core``).
         */
        val CORE_PACK_ID: Key = Key.key("nova", "core")
        
        internal val MCASSETS_DIR: Path = DATA_FOLDER.resolve("resource_pack/.mcassets")
        private val MCASSETS_DOWNLOAD_MUTEX = Mutex()
        
        private val customResourceFilters = ArrayList<ResourceFilter>()
        private val _configurations = ConcurrentHashMap<Key, ResourcePackConfiguration>()
        
        /**
         * The registered [ResourcePackConfigurations][ResourcePackConfiguration] by their [id][Key].
         */
        val configurations: Map<Key, ResourcePackConfiguration>
            get() = _configurations
        
        init {
            register(CORE_PACK_ID) {
                registerBuildData(::BasePacks)
                registerTask(BasePacks::Include)
                
                registerBuildData(::BlockStateContent)
                registerTask(BlockStateContent::PreLoadAll)
                registerTask(BlockStateContent::Write)
                
                registerBuildData(::FontContent)
                registerTask(FontContent::DiscoverAllFonts)
                registerTask(FontContent::Write)
                
                registerBuildData(::MovedFontContent)
                registerTask(MovedFontContent::Write)
                
                registerBuildData(::TextureIconContent)
                registerTask(TextureIconContent::Write)
                
                registerBuildData(::ItemModelContent)
                registerTask(ItemModelContent::GenerateItemDefinitions)
                registerTask(ItemModelContent::Write)
                
                registerBuildData(::ModelContent)
                registerTask(ModelContent::DiscoverAllModels)
                registerTask(ModelContent::Write)
                
                registerBuildData(::LanguageContent)
                registerTask(LanguageContent::Write)
                
                registerTask(::AtlasTask)
                registerTask(::BossBarOverlayTask)
                registerTask(::GuiTextureTask)
                registerTask(::MoveCharactersTask)
                registerTask(::WailaTask)
                registerTask(::BlockModelTask)
                registerTask(::EntityVariantTask)
                registerTask(::EquipmentTask)
                registerTask(::ExtractTask)
                registerBuildData(::TextureContent)
                registerTask(::TooltipStyleContent)
                registerTask(::CharSizeCalculator)
                registerTask(::SoundOverridesTask)
                registerTask(::PackMcMetaTask)
            }
        }
        
        /**
         * Registers a new [ResourcePackConfiguration] with [id] and [configures][configure] it.
         * @throws IllegalArgumentException If [id] is already in use.
         */
        fun register(id: Key, configure: ResourcePackConfiguration.() -> Unit) {
            require(id !in configurations) { "Id $id is already in use" }
            _configurations[id] = ResourcePackConfiguration(id).apply(configure)
        }
        
        /**
         * [Configures][configure] the existing [ResourcePackConfiguration] under [id].
         * @throws IllegalArgumentException If there is no [ResourcePackConfiguration] registered for [id].
         */
        fun configure(id: Key, configure: ResourcePackConfiguration.() -> Unit) {
            val configuration = configurations[id]
            requireNotNull(configuration) { "No ResourcePackBuilderFactory registered for id $id" }
            configuration.configure()
        }
        
        /**
         * Creates a new [ResourcePackBuilder] using the configuration under [id].
         * @throws IllegalArgumentException If there is no [ResourcePackConfiguration] registered for [id].
         */
        internal fun createBuilder(id: Key, extraListener: Audience? = null): ResourcePackBuilder {
            val configuration = configurations[id]
            requireNotNull(configuration) { "No ResourcePackBuilderFactory registered for id $id" }
            return configuration.create(extraListener)
        }
        
        /**
         * Builds and uploads the resource pack with the specified [id].
         * 
         * If [sendToPlayers] is `true`, the pack will also be sent to all online players that have the pack enabled.
         * 
         * If [extraListener] is not `null`, the log output of the build session will be forwarded to it.
         * 
         * @throws IllegalArgumentException If there is no [ResourcePackConfiguration] registered for [id].
         */
        suspend fun build(id: Key, sendToPlayers: Boolean = true, extraListener: Audience? = null) {
            val bin = createBuilder(id, extraListener).build()
            AutoUploadManager.uploadPack(id, bin)
            AutoCopier.copyToDestinations(id, bin)
            
            if (sendToPlayers)
                ResourcePackManager.handlePackUpdated(id)
        }
        
        /**
         * Registers the specified [filters].
         */
        fun registerResourceFilters(vararg filters: ResourceFilter) {
            customResourceFilters += filters
        }
        
        private suspend fun downloadMcAssets(): Unit = MCASSETS_DOWNLOAD_MUTEX.withLock {
            if (!MCASSETS_DIR.exists() || PermanentStorage.retrieve<Version>("minecraft_assets_version") != SERVER_VERSION) {
                MCASSETS_DIR.toFile().deleteRecursively()
                val downloader = MinecraftAssetsDownloader(
                    version = SERVER_VERSION.toString(omitZeros = true),
                    outputDirectory = MCASSETS_DIR,
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
                PermanentStorage.store("minecraft_assets_version", SERVER_VERSION)
            }
        }
        
    }
    
    private lateinit var fs: FileSystem
    private val buildDir: Path
        get() = fs.rootDirectories.first()
    
    @PublishedApi
    internal lateinit var data: List<PackBuildData>
    internal lateinit var tasks: Map<BuildStage, List<PackTask>>
    internal lateinit var zipper: PackZipper
    internal lateinit var postProcessors: List<PackPostProcessor>
    private val resourceFilters: Map<ResourceFilter.Stage, List<ResourceFilter>> =
        sequenceOf(CONFIG_RESOURCE_FILTERS, CORE_RESOURCE_FILTERS, customResourceFilters)
            .flatten().groupByTo(enumMap()) { it.stage }
    
    private val taskTimes = HashMap<PackTask, Duration>()
    private var totalTime: Duration = Duration.ZERO // fixme: total duration ends up being less than task sum durations, why?
    
    /**
     * The [AssetPacks][AssetPack] of all addons.
     */
    lateinit var assetPacks: List<AssetPack> private set
    
    internal suspend fun build(): ByteArray {
        logger.info("Building resource pack $id")
        buildPackPreWorld()
        return buildPackPostWorld()
    }
    
    internal suspend fun buildPackPreWorld() {
        fs = Jimfs.newFileSystem(Configuration.unix())
        try {
            totalTime += measureTime {
                downloadMcAssets()
                logTaskOrder()
                assetPacks = loadAssetPacks()
                logger.info("Asset packs (${assetPacks.size}): ${assetPacks.joinToString(transform = AssetPack::namespace)}")
                tasks[BuildStage.PRE_WORLD]?.forEach { runTaskTimed(it) }
            }
        } catch (t: Throwable) {
            fs.close()
            throw t
        }
    }
    
    internal suspend fun buildPackPostWorld(): ByteArray {
        check(fs.isOpen) { "FileSystem is closed" }
        try {
            totalTime += measureTime {
                tasks[BuildStage.POST_WORLD]?.forEach { runTaskTimed(it) }
                logger.info("Packing zip...")
                var bin = zipper.createZip()
                if (postProcessors.isNotEmpty()) {
                    logger.info("Running ${postProcessors.size} post-processor(s)...")
                    bin = postProcessors.fold(bin) { b, p -> p.process(b) }
                }
                logTaskTimes()
                return bin
            }
        } finally {
            fs.close()
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
    
    private fun logTaskOrder() {
        logger.info("Tasks (${tasks.values.sumOf(List<*>::size)}):")
        for ((stage, tasks) in tasks) {
            logger.info("  $stage (${tasks.size}):")
            for (task in tasks) {
                val skipped = task.toString() in SKIP_PACK_TASKS
                logger.info("    ${task::class.simpleNestedName}" + if (skipped) " (skipped)" else "")
            }
        }
    }
    
    private suspend fun runTaskTimed(task: PackTask) {
        taskTimes[task] = measureTime { task.run() }
    }
    
    private fun logTaskTimes() {
        logger.info("Resource pack built in ${totalTime}:")
        taskTimes.entries.asSequence()
            .sortedByDescending { it.value }
            .take(5)
            .forEach { (task, time) -> logger.info("  ${task::class.simpleNestedName}: $time") }
    }
    
    /**
     * Gets the resource filters for the specified [ResourceFilter.Stage].
     */
    fun getResourceFilters(stage: ResourceFilter.Stage): List<ResourceFilter> =
        resourceFilters[stage] ?: emptyList()
    
    /**
     * Retrieves the instantiated [PackBuildData] of the specified type.
     *
     * @throws IllegalArgumentException If no holder of the specified type is registered or hasn't been instantiated yet.
     */
    inline fun <reified T : PackBuildData> getBuildData(): T {
        val holder = data.firstOrNull { it is T }
            ?: throw IllegalArgumentException("No holder of type ${T::class.simpleName} is present")
        return holder as T
    }
    
    /**
     * Creates a [Lazy] that retrieves an instantiated [PackBuildData] of the specified type.
     */
    inline fun <reified T : PackBuildData> getBuildDataLazily(): Lazy<T> = lazy { getBuildData<T>() }
    
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
     *
     * Example paths: `pack.json`, `assets/minecraft/textures/block/dirt.png`
     */
    fun resolveVanilla(path: String): Path =
        MCASSETS_DIR.resolve(path)
    
    /**
     * Resolves the file under [path] in the resource pack.
     */
    fun resolve(path: ResourcePath<*>): Path =
        buildDir.resolve(path.filePath)
    
    /**
     * Resolves the corresponding `.mcmeta` file for the specified [path].
     */
    fun resolveMeta(path: ResourcePath<ResourceType.HasMcMeta>): Path =
        buildDir.resolve("${path.filePath}.mcmeta")
    
    /**
     * Resolves the file under [path] in the resource pack.
     *
     * Example paths: `pack.json`, `assets/minecraft/textures/block/dirt.png`
     */
    fun resolve(path: String): Path =
        buildDir.resolve(path)
    
    /**
     * Deserializes the JSON content of the file under [path] in the resource pack
     * to [V] using [json], or returns `null` if the file does not exist.
     */
    inline fun <reified V> readJson(path: ResourcePath<ResourceType.JsonFile>, json: Json = Json): V? {
        val file = resolve(path)
        return if (file.exists()) file.readJson(json) else null
    }
    
    /**
     * Deserializes the JSON content of the file under [path] in the vanilla minecraft assets.
     * to [V] using [json], or returns `null` if the file does not exist.
     * If an exception occurs during deserialization, it is logged and `null` is returned.
     */
    internal inline fun <reified V> readJsonVanillaCatching(path: ResourcePath<ResourceType.JsonFile>, json: Json = Json): V? {
        val file = resolveVanilla(path)
        if (file.exists()) {
            try {
                return file.readJson<V>(json)
            } catch (e: Exception) {
                logger.error("An exception occurred trying to parse $file", e)
            }
        }
        
        return null
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