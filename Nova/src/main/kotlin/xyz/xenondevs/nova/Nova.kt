package xyz.xenondevs.nova

import org.bstats.bukkit.Metrics
import org.bukkit.plugin.PluginManager
import org.bukkit.plugin.java.JavaPlugin
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.config.PermanentStorage
import xyz.xenondevs.nova.data.database.DatabaseManager
import xyz.xenondevs.nova.initialize.Initializer
import xyz.xenondevs.nova.ui.setGlobalIngredients
import xyz.xenondevs.nova.util.AsyncExecutor
import xyz.xenondevs.nova.util.data.Version
import xyz.xenondevs.particle.utils.ReflectionUtils
import java.util.logging.Logger

lateinit var NOVA: Nova
lateinit var LOGGER: Logger
lateinit var PLUGIN_MANAGER: PluginManager
var IS_VERSION_CHANGE: Boolean = false

class Nova : JavaPlugin() {

    val version = Version(description.version.removeSuffix("-SNAPSHOT"))
    val devBuild = description.version.contains("SNAPSHOT")
    val disableHandlers = ArrayList<() -> Unit>()
    val pluginFile
        get() = file
    var isUninstalled = false
    
    override fun onEnable() {
        NOVA = this
        ReflectionUtils.setPlugin(this)
        LOGGER = logger
        PLUGIN_MANAGER = server.pluginManager
        
        IS_VERSION_CHANGE = PermanentStorage.retrieve("last_version") { "0.1" } != description.version
        PermanentStorage.store("last_version", description.version)
        
        setGlobalIngredients()
        Metrics(this, 11927)
        NovaConfig.init()
        Initializer.init()
    }
    
    override fun onDisable() {
        disableHandlers.forEach {
            runCatching(it).onFailure(Throwable::printStackTrace)
        }
        DatabaseManager.disconnect()
        AsyncExecutor.shutdown()
    }
    
}