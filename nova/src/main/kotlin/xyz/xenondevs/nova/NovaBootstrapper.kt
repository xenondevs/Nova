@file:Suppress("UnstableApiUsage", "unused")

package xyz.xenondevs.nova

import io.papermc.paper.plugin.bootstrap.BootstrapContext
import io.papermc.paper.plugin.bootstrap.PluginBootstrap
import io.papermc.paper.plugin.bootstrap.PluginProviderContext
import io.papermc.paper.plugin.entrypoint.LaunchEntryPointHandler
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager
import io.papermc.paper.plugin.provider.type.paper.PaperPluginParent
import kotlinx.coroutines.debug.DebugProbes
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext
import org.bukkit.plugin.java.JavaPlugin
import xyz.xenondevs.commons.version.ClosedVersionRange
import xyz.xenondevs.commons.version.Version
import xyz.xenondevs.nova.config.Configs
import xyz.xenondevs.nova.config.PermanentStorage
import xyz.xenondevs.nova.initialize.Initializer
import xyz.xenondevs.nova.patch.Patcher
import xyz.xenondevs.nova.serialization.cbf.CbfSerializers
import xyz.xenondevs.nova.util.SERVER_VERSION
import xyz.xenondevs.nova.util.data.useZip
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists

private val REQUIRED_SERVER_VERSION: ClosedVersionRange = Version("1.21.7")..Version("1.21.8")
internal val IS_DEV_SERVER: Boolean = System.getProperty("NovaDev") != null
internal val PREVIOUS_NOVA_VERSION: Version? = PermanentStorage.retrieve<Version>("last_version")
internal val DATA_FOLDER = Path("plugins", "Nova")

internal lateinit var BOOTSTRAPPER: NovaBootstrapper private set
internal lateinit var LIFECYCLE_MANAGER: LifecycleEventManager<*>
internal lateinit var LOGGER: ComponentLogger private set
internal lateinit var NOVA_VERSION: Version private set
internal lateinit var NOVA_JAR: Path private set

internal class NovaBootstrapper : PluginBootstrap {
    
    /**
     * Numbers of addons that still await bootstrapping.
     */
    var remainingAddons = 0
        private set
    
    init {
        BOOTSTRAPPER = this
    }
    
    override fun bootstrap(context: BootstrapContext) {
        LIFECYCLE_MANAGER = context.lifecycleManager
        LOGGER = context.logger
        NOVA_VERSION = Version(context.pluginMeta.version)
        NOVA_JAR = context.pluginSource
        
        if (IS_DEV_SERVER)
            LOGGER.warn("Running in dev mode! Never use this on a production server!")
        
        // prevent execution on unsupported minecraft versions
        if (SERVER_VERSION !in REQUIRED_SERVER_VERSION) {
            throw Exception("Nova is not compatible with this version of Minecraft.\n" +
                "Nova v$NOVA_VERSION only runs on $REQUIRED_SERVER_VERSION.")
        }
        
        // prevent execution if the previously installed version is not compatible with this version
        if (PREVIOUS_NOVA_VERSION != null && PREVIOUS_NOVA_VERSION < Version("0.9")) {
            throw Exception("This version of Nova is not compatible with the version that was previously installed.\n" +
                "Please erase all data related to Nova and try again.")
        }
        
        // count addons
        remainingAddons = LaunchEntryPointHandler.INSTANCE.storage.asSequence()
            .flatMap { (_, storage) -> storage.registeredProviders }
            .filterIsInstance<PaperPluginParent.PaperBootstrapProvider>()
            .count { it.source.useZip { it.resolve("nova-addon.yml").exists() } }
        
        // Immediately start initializer if no addons are installed
        if (remainingAddons == 0) {
            init()
        }
    }
    
    fun handleAddonBootstrap(context: BootstrapContext) {
        if (--remainingAddons == 0) {
            LIFECYCLE_MANAGER = context.lifecycleManager
            init()
        }
    }
    
    private fun init() {
        try {
            // legacy data folder migration if updating from 0.17 or earlier
            if (PREVIOUS_NOVA_VERSION != null && PREVIOUS_NOVA_VERSION < Version("0.18-SNAPSHOT"))
                LegacyDataFolderMigrator.migrate()
            
            if (IS_DEV_SERVER) {
                DebugProbes.install()
                DebugProbes.enableCreationStackTraces = true
            }
            
            Patcher.run()
            Configs.extractDefaultConfig()
            CbfSerializers.register()
            Initializer.start()
        } catch (t: Throwable) {
            LOGGER.error("", t)
            (LogManager.getContext(false) as LoggerContext).stop() // flush log messages
            Runtime.getRuntime().halt(-1) // force-quit
        }
    }
    
    override fun createPlugin(context: PluginProviderContext): JavaPlugin = Nova
    
}