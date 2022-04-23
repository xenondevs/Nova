package xyz.xenondevs.nova.addon

import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.addon.loader.AddonLoader
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.resources.Resources
import xyz.xenondevs.nova.initialize.Initializable
import java.io.File

internal object AddonsLoader : Initializable() {
    
    override val inMainThread = true
    override val dependsOn = emptySet<Initializable>()
    
    override fun init() {
        LOGGER.info("Loading Addons...")
        AddonManager.loadAddons()
    }
    
}

internal object AddonsInitializer : Initializable() {
    
    override val inMainThread = true
    override val dependsOn = setOf(NovaConfig, Resources)
    
    override fun init() {
        LOGGER.info("Initializing Addons...")
        AddonManager.initializeAddons()
    }
    
}

internal object AddonManager {
    
    private val addonsDir = File(NOVA.dataFolder, "addons/")
    internal val loaders = ArrayList<AddonLoader>()
    internal val addons = LinkedHashMap<String, Addon>()
    
    init {
        addonsDir.mkdirs()
    }
    
    fun loadAddons() {
        addonsDir.listFiles()!!.forEach {
            if (it.isFile && it.extension == "jar") {
                val loader = AddonLoader(it)
                val description = loader.description
                
                val id = description.id
                if (!NamespacedId.isValidNamespace(id) || id == "nova") {
                    LOGGER.severe("Failed to load addon ${loader.file.name}: \"$id\" is not a valid id")
                    return@forEach
                }
                
                LOGGER.info("Loaded addon ${getAddonString(description)}")
                loaders += AddonLoader(it)
            }
        }
    }
    
    fun initializeAddons() {
        loaders.sortedBy { it.description }.forEach { loader ->
            val description = loader.description
            LOGGER.info("Initializing addon ${getAddonString(description)}")
            val addon = loader.load()
            addons[addon.description.id] = addon
            addon.init()
        }
    }
    
    fun enableAddons() {
        addons.values.forEach { addon ->
            LOGGER.info("Enabling addon ${getAddonString(addon.description)}")
            addon.onEnable()
        }
    }
    
    fun disableAddons() {
        addons.values.forEach { addon -> 
            LOGGER.info("Disabling addon ${getAddonString(addon.description)}")
            addon.onDisable()
        }
    }
    
    fun hasAddon(id: String) = id in addons
    
    private fun getAddonString(description: AddonDescription) =
        "\"${description.name}\" [${description.id}] v${description.version}"
    
}