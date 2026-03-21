package xyz.xenondevs.nova.config

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import xyz.xenondevs.invui.window.WindowManager
import xyz.xenondevs.nova.DATA_FOLDER
import xyz.xenondevs.nova.IS_DEV_SERVER
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA_JAR
import xyz.xenondevs.nova.addon.AddonBootstrapper
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.serialization.kotlinx.NOVA_SERIALIZERS_MODULE
import xyz.xenondevs.nova.util.AsyncExecutor
import xyz.xenondevs.nova.util.BukkitDispatcher
import xyz.xenondevs.nova.util.data.useZip
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchService
import kotlin.io.path.bufferedReader
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.isDirectory
import kotlin.io.path.relativeTo
import kotlin.io.path.walk

/**
 * The [ConfigStorage] used for Nova and its addons.
 * Use [ConfigStorage.get] to get a [ConfigProvider] for a specific config.
 * 
 * Usage:
 * ```
 * // plugins/example_addon/configs/config.yml with e.g. `some_value: 123`
 * val myReloadableValue by CONFIGS["example_addon:config"].entry<Int>("some_value")
 * ```
 */
val CONFIGS: ConfigStorage = ConfigStorage(NOVA_SERIALIZERS_MODULE, NovaConfigBackend)

/**
 * Nova's main config (`plugins/Nova/config.yml`). Equivalent to `CONFIGS["nova:config"]`.
 */
val MAIN_CONFIG: ConfigProvider by lazy { CONFIGS["nova:config"] }

@Deprecated("Use CONFIGS instead", ReplaceWith("CONFIGS"))
val Configs get() = CONFIGS

@InternalInit(stage = InternalInitStage.PRE_WORLD)
internal object NovaConfigBackend : ConfigBackend {
    
    internal fun extractAllConfigs() {
        val extractedConfigs = PermanentStorage.retrieve<MutableMap<Key, String>>("stored_configs") ?: HashMap()
        val extractor = ConfigExtractor(extractedConfigs)
        extractConfigs(extractor, "nova", NOVA_JAR, DATA_FOLDER)
        for (addon in AddonBootstrapper.addons) {
            extractConfigs(extractor, addon.namespace(), addon.file, addon.dataFolder)
        }
        PermanentStorage.store("stored_configs", extractedConfigs)
    }
    
    private fun extractConfigs(extractor: ConfigExtractor, namespace: String, zipFile: Path, dataFolder: Path) {
        zipFile.useZip { zip ->
            val configsDir = zip.resolve("configs/")
            configsDir.walk()
                .filter { !it.isDirectory() && it.extension.equals("yml", true) }
                .forEach { config ->
                    val relPath = config.relativeTo(configsDir).invariantSeparatorsPathString
                    val configId = Key.key(namespace, relPath.substringBeforeLast('.'))
                    extractConfig(extractor, config, dataFolder.resolve("configs").resolve(relPath), configId)
                }
        }
    }
    
    private fun extractConfig(extractor: ConfigExtractor, from: Path, to: Path, id: Key) {
        extractor.extract(id, from, to)
        
        CONFIGS[id] // get config now to load it into memory
    }
    
    private fun resolveConfigPath(id: Key): Path {
        val dataFolder = when (id.namespace()) {
            "nova" -> DATA_FOLDER
            else -> AddonBootstrapper.addons.firstOrNull { it.namespace() == id.namespace() }?.dataFolder
                ?: throw IllegalArgumentException("No addon with id ${id.namespace()} found")
        }
        return dataFolder.resolve("configs").resolve(id.value() + ".yml")
    }
    
    override fun load(id: Key): JsonElement? {
        try {
            val path = resolveConfigPath(id)
            if (path.exists()) {
                ConfigWatcher.watchConfig(path)
                return path.bufferedReader().use(::readYamlAsJson)
            } else {
                return JsonObject(emptyMap())
            }
        } catch (e: Exception) {
            LOGGER.error("Failed to load config '$id': ${e.message}")
            return null
        }
    }
    
    override fun getLastModified(id: Key): Long {
        val path = resolveConfigPath(id)
        if (!path.exists())
            return 0L
        return path.getLastModifiedTime().toMillis()
    }
    
    override fun onError(id: Key, path: List<String>, exception: SerializationException) {
        LOGGER.error("Failed to read '${path.joinToString(" > ")}' in config '$id': ${exception.message}")
    }
    
    override fun postReload() {
        for (player in Bukkit.getOnlinePlayers()) {
            player.updateInventory()
        }
        
        for (window in WindowManager.getInstance().windows) {
            window.sendAllDataToViewer()
        }
    }
    
}

@InternalInit(stage = InternalInitStage.POST_WORLD)
internal object ConfigValidator {
    
    @InitFun
    private fun validate() {
        CONFIGS.resolveEntries()
    }
    
}

@InternalInit(stage = InternalInitStage.POST_WORLD)
internal object ConfigWatcher {
    
    private val watchService: WatchService? =
        if (IS_DEV_SERVER) FileSystems.getDefault().newWatchService() else null
    private val dirs = HashSet<Path>()
    
    fun watchConfig(config: Path) {
        if (config.exists())
            dirs.add(config.parent)
    }
    
    @InitFun
    private fun startWatching() {
        if (watchService == null)
            return
        
        for (dir in dirs) {
            dir.register(
                watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_DELETE,
                StandardWatchEventKinds.ENTRY_MODIFY
            )
        }
        
        CoroutineScope(AsyncExecutor.SUPERVISOR + Dispatchers.IO).launch {
            while (isActive) {
                val key = watchService.take()
                key.pollEvents()
                
                withContext(BukkitDispatcher) {
                    LOGGER.info("Detected config change, reloading configs...")
                    val reloadedConfigs = CONFIGS.reload()
                    LOGGER.info("Reloaded configs: ${reloadedConfigs.joinToString(", ")}")
                }
                
                key.pollEvents()
                key.reset()
            }
        }
    }
    
}