package xyz.xenondevs.nova.resources.copy

import kotlinx.coroutines.runBlocking
import org.spongepowered.configurate.ConfigurationNode
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.config.MAIN_CONFIG
import xyz.xenondevs.nova.config.PermanentStorage
import xyz.xenondevs.nova.config.strongNode
import xyz.xenondevs.nova.initialize.*
import xyz.xenondevs.nova.resources.ResourceGeneration
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.util.data.getList
import java.io.File
import java.nio.file.Path
import kotlin.io.path.copyTo
import kotlin.io.path.createParentDirectories
import kotlin.io.path.exists

@InternalInit(
    stage = InternalInitStage.POST_WORLD,
    dispatcher = Dispatcher.ASYNC,
    dependsOn = [ResourceGeneration.PostWorld::class]
)
internal object AutoCopyManager {
    
    var enabled = false
        private set
    var wasRegenerated = false
    private var shouldCreateDirectories = false

    private var destinations: List<File>? = filesFromPaths(PermanentStorage.retrieve("copyToDestinations") as List<String>?)
        set(value) {
            field = value
            PermanentStorage.store("copyToDestinations", filesToPaths(value))
        }
    private var lastConfig: Int? = PermanentStorage.retrieve("lastCopyConfig")
        set(value) {
            field = value
            PermanentStorage.store("lastCopyConfig", value)
        }

    @InitFun
    private fun init() {
        val cfg = MAIN_CONFIG.strongNode("resource_pack")
        cfg.subscribe { disable(); enable(it, fromReload = true) }
        enable(cfg.get(), fromReload = false)
    }

    private fun enable(cfg: ConfigurationNode, fromReload: Boolean) {
        val autoCopyCfg = cfg.node("auto_copy")
        enabled = autoCopyCfg.node("enabled").boolean

        if (autoCopyCfg.hasChild("destinations")) {
            val destinations = autoCopyCfg.node("destinations").getList<File>()
            if (!destinations.isNullOrEmpty()) {
                if (this.destinations != destinations) {
                    this.destinations = destinations
                }
                return
            }
        }

        if (!enabled) {
            this.destinations = emptyList()
            return
        }

        shouldCreateDirectories = autoCopyCfg.node("create_directories").boolean

        val configHash = autoCopyCfg.hashCode()
        if (wasRegenerated || lastConfig != configHash) {
            wasRegenerated = false
            runBlocking {
                val destinations = copyPack(ResourcePackBuilder.RESOURCE_PACK_FILE)
                if (destinations.isNullOrEmpty())
                    LOGGER.warn("The resource pack was not copied. (Misconfigured auto copier?)")
            }
            lastConfig = configHash
        }
    }

    @DisableFun(dispatcher = Dispatcher.ASYNC)
    private fun disable() {

    }

    fun copyPack(pack: Path): List<File>? {
        val resultFiles = mutableListOf<File>()

        try {
            require(pack.exists()) { pack + " not found!" }

            destinations?.forEach {
                val destinationPath = if (shouldCreateDirectories) it.toPath().createParentDirectories() else it.toPath()
                val copiedToPath = pack.copyTo(destinationPath.normalize(), true)
                resultFiles.add(copiedToPath.toFile().normalize())
            }

            this.destinations = resultFiles
        } catch (e: Exception) {
            LOGGER.error("Failed to copy the resource pack!", e)
        }

        return destinations
    }

    fun filesToPaths(files: List<File>?): List<String> {
        val paths = mutableListOf<String>()
        files?.forEach {
            paths.add(it.path)
        }
        return paths
    }

    fun filesFromPaths(paths: List<String>?): List<File> {
        val files = mutableListOf<File>()
        paths?.forEach {
            files.add(File(it))
        }
        return files
    }
    
}