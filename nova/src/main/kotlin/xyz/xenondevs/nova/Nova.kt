package xyz.xenondevs.nova

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.gson.*
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin
import xyz.xenondevs.nova.addon.AddonManager
import xyz.xenondevs.nova.api.protection.ProtectionIntegration
import xyz.xenondevs.nova.data.config.DEFAULT_CONFIG
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.config.PermanentStorage
import xyz.xenondevs.nova.initialize.Initializer
import xyz.xenondevs.nova.integration.protection.ProtectionManager
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import xyz.xenondevs.nova.tileentity.TileEntityManager
import xyz.xenondevs.nova.ui.waila.WailaManager
import xyz.xenondevs.nova.util.ServerUtils
import xyz.xenondevs.nova.util.data.Version
import xyz.xenondevs.nova.world.block.BlockManager
import java.io.File
import java.util.logging.Level
import java.util.logging.Logger
import xyz.xenondevs.nova.api.Nova as INova
import xyz.xenondevs.nova.api.block.BlockManager as IBlockManager
import xyz.xenondevs.nova.api.material.NovaMaterialRegistry as INovaMaterialRegistry
import xyz.xenondevs.nova.api.player.WailaManager as IWailaManager
import xyz.xenondevs.nova.api.tileentity.TileEntityManager as ITileEntityManager

private val REQUIRED_SERVER_VERSION = Version("1.19.1")..Version("1.19.2")

internal lateinit var NOVA: Nova
    private set

internal val IS_DEV_SERVER: Boolean = System.getProperty("NovaDev") != null

internal val HTTP_CLIENT = HttpClient(CIO) {
    install(ContentNegotiation) { gson() }
    install(HttpTimeout) {
        connectTimeoutMillis = 10_000
        requestTimeoutMillis = HttpTimeout.INFINITE_TIMEOUT_MS
    }
    expectSuccess = false
}

internal lateinit var LOGGER: Logger
    private set

class Nova(internal val loader: JavaPlugin, val pluginFile: File) : Plugin by loader, INova {
    
    val version = Version(loader.description.version)
    val lastVersion = PermanentStorage.retrieveOrNull<String>("last_version")?.let { if (it == "0.1") Version("0.10") else Version(it) }
    val isVersionChange = lastVersion != null && lastVersion != version
    val isDevServer = IS_DEV_SERVER
    
    override val blockManager: IBlockManager = BlockManager
    override val tileEntityManager: ITileEntityManager = TileEntityManager
    override val materialRegistry: INovaMaterialRegistry = NovaMaterialRegistry
    override val wailaManager: IWailaManager = WailaManager
    
    internal val disableHandlers = ArrayList<() -> Unit>()
    
    override fun onEnable() {
        NOVA = this
        LOGGER = loader.logger
        
        if (IS_DEV_SERVER)
            LOGGER.warning("Running in dev mode! Never use this on a production server!")
        
        NovaConfig.loadDefaultConfig()
        if (checkStartup()) {
            Initializer.initPreWorld()
        }
    }
    
    private fun checkStartup(): Boolean {
        // prevent execution on unsupported minecraft versions
        if (Version.SERVER_VERSION !in REQUIRED_SERVER_VERSION) {
            LOGGER.severe("Nova is not compatible with this version of Minecraft!")
            LOGGER.severe("Nova only runs on $REQUIRED_SERVER_VERSION.")
            Bukkit.getPluginManager().disablePlugin(loader)
            return false
        }
        
        // prevent execution if the previously installed version is not compatible with this version
        if (lastVersion != null && lastVersion < Version("0.9")) {
            LOGGER.severe("This version of Nova is not compatible with the version that was previously installed.")
            LOGGER.severe("Please erase all data related to Nova and try again.")
            Bukkit.getPluginManager().disablePlugin(loader)
            return false
        }
        
        // prevent reloading if this server is using an agent or Nova was updated
        if (!IS_DEV_SERVER && ServerUtils.isReload && (DEFAULT_CONFIG.getBoolean("use_agent") || (lastVersion != version))) {
            LOGGER.severe("========================================================================================")
            LOGGER.severe("!RELOADING IS NOT SUPPORTED WHEN USING AN AGENT OR UPDATING. PLEASE RESTART YOUR SERVER!")
            LOGGER.severe("========================================================================================")
            Bukkit.getPluginManager().disablePlugin(loader)
            return false
        }
        
        return true
    }
    
    override fun onDisable() {
        if (Initializer.isDone) {
            AddonManager.disableAddons()
            Initializer.disable()
            disableHandlers.forEach {
                runCatching(it).onFailure { ex ->
                    LOGGER.log(Level.SEVERE, "An exception occurred while running a disable handler", ex)
                }
            }
        }
    }
    
    override fun registerProtectionIntegration(integration: ProtectionIntegration) {
        ProtectionManager.integrations.add(integration)
    }
    
}