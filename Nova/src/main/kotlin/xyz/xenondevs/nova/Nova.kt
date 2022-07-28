package xyz.xenondevs.nova

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.gson.*
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import xyz.xenondevs.nova.addon.AddonManager
import xyz.xenondevs.nova.api.protection.ProtectionIntegration
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.config.PermanentStorage
import xyz.xenondevs.nova.initialize.Initializer
import xyz.xenondevs.nova.integration.protection.ProtectionManager
import xyz.xenondevs.nova.material.CoreItems
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import xyz.xenondevs.nova.tileentity.TileEntityManager
import xyz.xenondevs.nova.util.data.Version
import xyz.xenondevs.particle.utils.ReflectionUtils
import java.util.logging.Level
import java.util.logging.Logger
import xyz.xenondevs.nova.api.Nova as INova

private const val REQUIRED_SERVER_VERSION = "1.19.1"

lateinit var NOVA: Nova
internal var IS_VERSION_CHANGE: Boolean = false
internal val HTTP_CLIENT = HttpClient(CIO) {
    install(ContentNegotiation) { gson() }
    expectSuccess = false
}
internal lateinit var LOGGER: Logger

class Nova : JavaPlugin(), INova {
    
    private var fullyEnabled = false
    
    val version = Version(description.version)
    val isDevBuild = description.version.contains("SNAPSHOT")
    internal val disableHandlers = ArrayList<() -> Unit>()
    val pluginFile
        get() = file
    
    override val tileEntityManager: TileEntityManager
        get() = TileEntityManager
    override val materialRegistry: NovaMaterialRegistry
        get() = NovaMaterialRegistry
    
    override fun onEnable() {
        NOVA = this
        LOGGER = logger
        ReflectionUtils.setPlugin(this)
        
        // prevent execution on unsupported minecraft versions
        if (Version.SERVER_VERSION != Version(REQUIRED_SERVER_VERSION)) {
            LOGGER.severe("Nova is not compatible with this version of Minecraft!")
            LOGGER.severe("Nova only runs on $REQUIRED_SERVER_VERSION.")
            Bukkit.getPluginManager().disablePlugin(this)
            return
        }
        
        // prevent execution if the previously installed version is not compatible with this version
        val lastVersion = PermanentStorage.retrieveOrNull<String>("last_version")?.let(::Version)
        if (lastVersion != null && lastVersion < Version("0.9")) {
            LOGGER.severe("This version of Nova is not compatible with the version that was previously installed.")
            LOGGER.severe("Please erase all data related to Nova and try again.")
            Bukkit.getPluginManager().disablePlugin(this)
            return
        }
        
        IS_VERSION_CHANGE = PermanentStorage.retrieve("last_version") { "0.1" } != description.version
        PermanentStorage.store("last_version", description.version)
        
        NovaConfig.loadDefaultConfig()
        CoreItems.init()
        Initializer.init()
        
        fullyEnabled = true
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