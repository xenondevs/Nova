package xyz.xenondevs.nova.addon.loader

import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.initialize.Initializable
import xyz.xenondevs.nova.tileentity.network.NetworkManager
import java.io.File

internal object AddonsInitializer : Initializable() {
    
    override val inMainThread = true
    override val dependsOn: Initializable? = null
    
    override fun init() {
        LOGGER.info("Initializing Addons...")
        AddonManager.init()
    }
    
}

internal object AddonsLoader : Initializable() {
    
    override val inMainThread = true
    override val dependsOn = NetworkManager
    
    override fun init() {
        LOGGER.info("Loading Addons...")
        AddonManager.loadAddons()
    }
    
}

internal object AddonManager {
    
    private val addonsDir = File(NOVA.dataFolder, "addons/")
    internal val loaders = ArrayList<AddonLoader>()
    private val addons = HashMap<String, Addon>()
    
    fun init() {
        addonsDir.mkdirs()
        
        addonsDir.listFiles()!!.forEach {
            if (it.isFile && it.extension == "jar")
                loaders += AddonLoader(it)
        }
    }
    
    fun loadAddons() {
        // TODO: sort based on depend and softdepend
        loaders.forEach { loader ->
            val description = loader.description
            LOGGER.info("Loading addon \"${description.name}\" [${description.id}] v${description.version}")
            val addon = loader.load()
            addons[addon.description.id] = addon
            addon.onEnable()
        }
    }
    
    fun hasAddon(id: String) = id in addons
    
}