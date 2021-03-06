package xyz.xenondevs.nova.addon

import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.addon.loader.AddonLoader
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.resources.Resources
import xyz.xenondevs.nova.initialize.Initializable
import java.io.File
import java.util.logging.Level

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

object AddonManager {
    
    private val DISALLOWED_IDS = hashSetOf("nova", "minecraft", "itemsadder", "oraxen", "mmoitems")
    
    internal val addonsDir = File(NOVA.dataFolder, "addons/")
    internal val loaders = HashMap<String, AddonLoader>()
    internal val addons = LinkedHashMap<String, Addon>()
    
    internal var addonsHashCode = -1
        private set
    
    init {
        addonsDir.mkdirs()
    }
    
    fun hasAddon(id: String) = id in addons
    
    fun getAddon(id: String) = addons[id]
    
    internal fun loadAddons() {
        addonsDir.listFiles()!!.forEach {
            try {
                if (it.isFile && it.extension == "jar") {
                    val loader = AddonLoader(it)
                    val description = loader.description
                    
                    val id = description.id
                    if (!NamespacedId.PART_PATTERN.matches(id) || id in DISALLOWED_IDS) {
                        loader.logger.severe("Failed to load ${it.name}: \"$id\" is not a valid id")
                        return@forEach
                    }
                    
                    loader.logger.info("Loaded ${getAddonString(description)}")
                    loaders[id] = AddonLoader(it)
                }
            } catch (t: Throwable) {
                LOGGER.log(Level.SEVERE, "An exception occurred trying to load ${it.name} (Is it up to date?)", t)
            }
        }
        
        generateAddonsHashCode()
    }
    
    internal fun initializeAddons() {
        loaders.values.sortedBy { it.description }.forEach { loader ->
            val description = loader.description
            loader.logger.info("Initializing ${getAddonString(description)}")
            
            val missingDependencies = loader.description.depend.filter { it !in loaders.keys }
            if (missingDependencies.isNotEmpty()) {
                loader.logger.log(Level.SEVERE, "Failed to initialize ${getAddonString(description)}: Missing addon(s): " +
                    missingDependencies.joinToString { "[$it]" })
                return@forEach
            }
            
            try {
                loader.classLoader.setDependencyClassLoaders()
                val addon = loader.load()
                addons[addon.description.id] = addon
                addon.init()
            } catch (t: Throwable) {
                loader.logger.log(Level.SEVERE, "An exception occurred trying to initialize ${getAddonString(description)} (Is it up to date?)", t)
            }
        }
    }
    
    internal fun enableAddons() {
        addons.values.forEach { addon ->
            addon.logger.info("Enabling ${getAddonString(addon.description)}")
            
            try {
                addon.onEnable()
            } catch (t: Throwable) {
                addon.logger.log(Level.SEVERE, "An exception occurred trying to enable ${getAddonString(addon.description)} (Is it up to date?)", t)
            }
        }
    }
    
    internal fun disableAddons() {
        addons.values.forEach { addon ->
            try {
                addon.logger.info("Disabling ${getAddonString(addon.description)}")
                addon.onDisable()
            } catch (t: Throwable) {
                addon.logger.log(Level.SEVERE, "An exception occurred trying to disable ${getAddonString(addon.description)}", t)
            }
        }
    }
    
    private fun getAddonString(description: AddonDescription) =
        "${description.name} [${description.id}] v${description.version}"
    
    private fun generateAddonsHashCode() {
        var result = NOVA.version.hashCode()
        loaders.values.forEach {
            val description = it.description
            result = result * 31 + description.id.hashCode()
            result = result * 31 + description.version.hashCode()
        }
        
        addonsHashCode = result
    }
    
}