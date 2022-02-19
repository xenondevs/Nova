package xyz.xenondevs.nova.addon.loader

import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.data.resources.Resources
import xyz.xenondevs.nova.initialize.Initializable
import java.io.File

internal object AddonsInitializer : Initializable() {
    
    override val inMainThread = true
    override val dependsOn = emptySet<Initializable>()
    
    override fun init() {
        LOGGER.info("Initializing Addons...")
        AddonManager.init()
    }
    
}

internal object AddonsLoader : Initializable() {
    
    override val inMainThread = true
    override val dependsOn = setOf(Resources)
    
    override fun init() {
        LOGGER.info("Loading Addons...")
        AddonManager.loadAddons()
    }
    
}

internal object AddonManager {
    
    private val ID_PATTERN = Regex("""^[a-z][a-z0-9_]*$""")
    
    private val addonsDir = File(NOVA.dataFolder, "addons/")
    internal val loaders = ArrayList<AddonLoader>()
    internal val addons = HashMap<String, Addon>()
    
    fun init() {
        addonsDir.mkdirs()
        
        addonsDir.listFiles()!!.forEach {
            if (it.isFile && it.extension == "jar") {
                val loader = AddonLoader(it)
                val description = loader.description
                
                @Suppress("SENSELESS_COMPARISON") // gson deserialization can do this
                if (description.id == null || description.name == null || description.version == null || description.main == null) {
                    LOGGER.severe("Failed to load addon ${loader.file.name}: Invalid description")
                    return@forEach
                } else if (!description.id.matches(ID_PATTERN)) {
                    LOGGER.severe("Failed to load addon ${loader.file.name}: Id ${description.id} does not match $ID_PATTERN")
                    return@forEach
                } else if (description.id == "nova") {
                    LOGGER.severe("Failed to load addon ${loader.file.name}: 'nova' is not a valid id")
                    return@forEach
                }
                
                loaders += AddonLoader(it)
            }
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