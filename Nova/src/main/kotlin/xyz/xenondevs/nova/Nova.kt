package xyz.xenondevs.nova

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.gson.*
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import xyz.xenondevs.nova.addon.AddonManager
import xyz.xenondevs.nova.api.protection.ProtectionIntegration
import xyz.xenondevs.nova.data.config.DEFAULT_CONFIG
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.config.PermanentStorage
import xyz.xenondevs.nova.initialize.Initializer
import xyz.xenondevs.nova.integration.protection.ProtectionManager
import xyz.xenondevs.nova.material.CoreItems
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import xyz.xenondevs.nova.tileentity.TileEntityManager
import xyz.xenondevs.nova.util.ServerUtils
import xyz.xenondevs.nova.util.data.Version
import xyz.xenondevs.nova.world.block.BlockManager
import java.util.logging.Level
import java.util.logging.Logger
import xyz.xenondevs.nova.api.Nova as INova
import xyz.xenondevs.particle.utils.ReflectionUtils as ParticleLibReflectionUtils

private val REQUIRED_SERVER_VERSION = Version("1.19.1")

lateinit var NOVA: Nova
internal var IS_DEV_SERVER: Boolean = System.getProperty("NovaDev") != null
internal val HTTP_CLIENT = HttpClient(CIO) {
    install(ContentNegotiation) { gson() }
    expectSuccess = false
}
internal lateinit var LOGGER: Logger

class Nova : JavaPlugin(), INova {
    
    val version = Version(description.version)
    val lastVersion = PermanentStorage.retrieveOrNull<String>("last_version")?.let(::Version)
    val isVersionChange = lastVersion != null && lastVersion != version
    val isDevServer = IS_DEV_SERVER
    
    val pluginFile = file
    override val blockManager = BlockManager
    override val tileEntityManager = TileEntityManager
    override val materialRegistry = NovaMaterialRegistry
    
    internal val disableHandlers = ArrayList<() -> Unit>()
    private var fullyEnabled = false
    
    override fun onEnable() {
        NOVA = this
        LOGGER = logger
        
        if (IS_DEV_SERVER)
            LOGGER.warning("Running in dev mode! Never use this on a production server!")
        
        ParticleLibReflectionUtils.setPlugin(this)
        NovaConfig.loadDefaultConfig()
        
        if (checkStartup()) {
            CoreItems.init()
            Initializer.init()
            
            PermanentStorage.store("last_version", description.version)
            fullyEnabled = true
        }
    }
    
    private fun checkStartup(): Boolean {
        // prevent execution on unsupported minecraft versions
        if (Version.SERVER_VERSION != REQUIRED_SERVER_VERSION) {
            LOGGER.severe("Nova is not compatible with this version of Minecraft!")
            LOGGER.severe("Nova only runs on $REQUIRED_SERVER_VERSION.")
            Bukkit.getPluginManager().disablePlugin(this)
            return false
        }
        
        // prevent execution if the previously installed version is not compatible with this version
        if (lastVersion != null && lastVersion < Version("0.9")) {
            LOGGER.severe("This version of Nova is not compatible with the version that was previously installed.")
            LOGGER.severe("Please erase all data related to Nova and try again.")
            Bukkit.getPluginManager().disablePlugin(this)
            return false
        }
        
        // prevent reloading if this server is using an agent or Nova was updated
        if (!IS_DEV_SERVER && ServerUtils.isReload && (DEFAULT_CONFIG.getBoolean("use_agent") || (lastVersion != version))) {
            LOGGER.severe("========================================================================================")
            LOGGER.severe("!RELOADING IS NOT SUPPORTED WHEN USING AN AGENT OR UPDATING. PLEASE RESTART YOUR SERVER!")
            LOGGER.severe("========================================================================================")
            Bukkit.getPluginManager().disablePlugin(this)
            return false
        }
        
        return true
    }
    
    override fun onDisable() {
        if (fullyEnabled) {
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