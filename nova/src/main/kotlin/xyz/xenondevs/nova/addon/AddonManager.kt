package xyz.xenondevs.nova.addon

import xyz.xenondevs.commons.collections.CollectionUtils
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.addon.loader.AddonLoader
import xyz.xenondevs.nova.addon.loader.AddonLoaderPools
import xyz.xenondevs.nova.data.config.Configs
import xyz.xenondevs.nova.initialize.Init
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InitializableClass
import xyz.xenondevs.nova.initialize.InitializationException
import xyz.xenondevs.nova.initialize.Initializer
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.transformer.Patcher
import xyz.xenondevs.nova.util.data.JarUtils
import java.io.File
import java.util.logging.Level

private val DISALLOWED_NAMESPACES = hashSetOf("minecraft", "nova", "itemsadder", "oraxen", "mmoitems")
private val ADDON_ID_PATTERN = Regex("""^[a-z][a-z\d_-]*$""")

/**
 * Loads all addon jars.
 */
@InternalInit(stage = InternalInitStage.PRE_WORLD)
internal object AddonsLoader {
    
    @InitFun
    private fun loadAddons() {
        LOGGER.info("Loading Addons...")
        
        AddonManager.addonsDir.listFiles()!!.forEach {
            try {
                if (it.isFile && it.extension == "jar") {
                    val loader = AddonLoader(it)
                    val description = loader.description
                    
                    val id = description.id
                    if (!ADDON_ID_PATTERN.matches(id) || id in DISALLOWED_NAMESPACES) {
                        loader.logger.severe("Failed to load ${it.name}: \"$id\" is not a valid id")
                        return@forEach
                    }
                    
                    if (id in AddonManager.loaders)
                        throw InitializationException("Duplicate addon id $id for ${loader.file} and ${AddonManager.loaders[id]!!.file}")
                    
                    loader.logger.info("Loaded ${getAddonString(description)}")
                    AddonManager.loaders[id] = loader
                }
            } catch (i: InitializationException) {
                throw InitializationException("Could not load addon ${it.name}: ${i.message}")
            } catch (t: Throwable) {
                throw AddonLoadException(it, t)
            }
        }
        
        AddonManager.loaders.values.forEach { loader ->
            val description = loader.description
            
            val missingDependencies = description.depend.filter { it !in AddonManager.loaders.keys }
            if (missingDependencies.isNotEmpty()) {
                throw InitializationException("Failed to initialize ${getAddonString(description)}: Missing addon(s): " +
                    missingDependencies.joinToString { "[$it]" })
            }
        }
    }
    
}

/**
 * Initializes addon library loaders, calls [Addon.init] and initializes classes annotated with [@Init][Init].
 */
@InternalInit(
    stage = InternalInitStage.PRE_WORLD,
    dependsOn = [Configs::class, Patcher::class]
)
internal object AddonsInitializer {
    
    @InitFun
    internal fun initializeAddons() {
        LOGGER.info("Initializing Addons...")
        
        // pool class loaders
        val classLoaders = AddonLoaderPools.createPooledClassLoaders(AddonManager.loaders.values)
        
        // init addons ordered by dependencies
        val addonLoaders = CollectionUtils.sortDependencies(AddonManager.loaders.values) {
            (it.description.depend + it.description.softdepend).mapNotNull(AddonManager.loaders::get).toSet()
        }
        
        val initClasses = ArrayList<InitializableClass>()
        
        addonLoaders.forEach { loader ->
            val description = loader.description
            loader.logger.info("Initializing ${getAddonString(description)}")
            
            try {
                val classLoader = classLoaders[loader]
                    ?: throw IllegalStateException("No class loader for addon")
                
                // create addon instance
                val addon = loader.load(classLoader)
                AddonManager.addons[addon.description.id] = addon
                
                // init addon
                addon.init()
                initClasses += JarUtils.findAnnotatedClasses(addon.addonFile, Init::class)
                    .map { (clazz, annotation) -> InitializableClass.fromAddonAnnotation(classLoader, clazz, annotation) }
            } catch (t: Throwable) {
                throw AddonInitializeException(loader, t)
            }
        }
        
        Initializer.addInitClasses(initClasses)
    }
    
}

object AddonManager {
    
    internal val addonsDir = File(NOVA.dataFolder, "addons/")
    internal val loaders = HashMap<String, AddonLoader>()
    internal val addons = LinkedHashMap<String, Addon>()
    
    init {
        addonsDir.mkdirs()
    }
    
    fun hasAddon(id: String) = id in addons
    
    fun getAddon(id: String) = addons[id]
    
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
    
}

private fun getAddonString(description: AddonDescription) =
    "${description.name} [${description.id}] v${description.version}"

private class AddonLoadException(file: File, t: Throwable) : Exception(
    "An exception occurred trying to load ${file.name}. (Is it up to date?)",
    t
)

private class AddonInitializeException(loader: AddonLoader, t: Throwable) : Exception(
    "An exception occurred trying to initialize ${getAddonString(loader.description)}. (Is it up to date?)",
    t
)