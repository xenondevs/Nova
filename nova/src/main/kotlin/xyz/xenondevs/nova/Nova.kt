@file:Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")

package xyz.xenondevs.nova

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.gson.*
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import xyz.xenondevs.nova.addon.AddonManager
import xyz.xenondevs.nova.api.ApiBlockManager
import xyz.xenondevs.nova.api.ApiBlockRegistry
import xyz.xenondevs.nova.api.ApiItemRegistry
import xyz.xenondevs.nova.api.ApiTileEntityManager
import xyz.xenondevs.nova.api.NovaMaterialRegistry
import xyz.xenondevs.nova.api.protection.ProtectionIntegration
import xyz.xenondevs.nova.config.PermanentStorage
import xyz.xenondevs.nova.initialize.Initializer
import xyz.xenondevs.nova.integration.protection.ProtectionManager
import xyz.xenondevs.nova.ui.waila.WailaManager
import xyz.xenondevs.nova.util.ServerUtils
import xyz.xenondevs.nova.util.data.Version
import java.util.logging.Level
import java.util.logging.Logger
import xyz.xenondevs.nova.api.Nova as INova
import xyz.xenondevs.nova.api.block.BlockManager as IBlockManager
import xyz.xenondevs.nova.api.block.NovaBlockRegistry as INovaBlockRegistry
import xyz.xenondevs.nova.api.item.NovaItemRegistry as INovaItemRegistry
import xyz.xenondevs.nova.api.material.NovaMaterialRegistry as INovaMaterialRegistry
import xyz.xenondevs.nova.api.player.WailaManager as IWailaManager
import xyz.xenondevs.nova.api.tileentity.TileEntityManager as ITileEntityManager

private val REQUIRED_SERVER_VERSION = Version("1.21.1")..Version("1.21.1")
internal val IS_DEV_SERVER: Boolean = System.getProperty("NovaDev") != null
internal lateinit var NOVA: Nova private set
internal lateinit var LOGGER: Logger private set

internal val HTTP_CLIENT = HttpClient(CIO) {
    install(ContentNegotiation) { gson() }
    install(HttpTimeout) {
        connectTimeoutMillis = 10_000
        requestTimeoutMillis = HttpTimeout.INFINITE_TIMEOUT_MS
    }
    expectSuccess = false
}

internal class Nova : JavaPlugin(), INova {
    
    val version = Version(description.version)
    val lastVersion = PermanentStorage.retrieveOrNull<Version>("last_version")
    val novaJar = file
    
    internal val disableHandlers = ArrayList<() -> Unit>()
    
    init {
        NOVA = this
        LOGGER = logger
    }
    
    override fun onEnable() {
        if (IS_DEV_SERVER)
            LOGGER.warning("Running in dev mode! Never use this on a production server!")
        
        if (checkStartup()) {
            Initializer.start()
        }
    }
    
    private fun checkStartup(): Boolean {
        // prevent execution on unsupported minecraft versions
        if (Version.SERVER_VERSION !in REQUIRED_SERVER_VERSION) {
            LOGGER.severe("Nova is not compatible with this version of Minecraft!")
            LOGGER.severe("Nova v$version only runs on $REQUIRED_SERVER_VERSION.")
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
        
        if (ServerUtils.isReload()) {
            LOGGER.severe("====================================================")
            LOGGER.severe("RELOADING IS NOT SUPPORTED, SHUTTING DOWN THE SERVER")
            LOGGER.severe("====================================================")
            Bukkit.shutdown()
        }
    }
    
    //<editor-fold desc="nova-api", defaultstate="collapsed">
    override fun getBlockManager(): IBlockManager = ApiBlockManager
    override fun getTileEntityManager(): ITileEntityManager = ApiTileEntityManager
    override fun getMaterialRegistry(): INovaMaterialRegistry = NovaMaterialRegistry
    override fun getBlockRegistry(): INovaBlockRegistry = ApiBlockRegistry
    override fun getItemRegistry(): INovaItemRegistry = ApiItemRegistry
    override fun getWailaManager(): IWailaManager = WailaManager
    
    override fun registerProtectionIntegration(integration: ProtectionIntegration) {
        ProtectionManager.integrations.add(integration)
    }
    //</editor-fold>
    
}