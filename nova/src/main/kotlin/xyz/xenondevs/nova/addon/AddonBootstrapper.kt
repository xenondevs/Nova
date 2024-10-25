@file:Suppress("UnstableApiUsage")

package xyz.xenondevs.nova.addon;

import io.papermc.paper.plugin.bootstrap.PluginProviderContext
import io.papermc.paper.plugin.entrypoint.LaunchEntryPointHandler
import org.bukkit.plugin.java.JavaPlugin
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import xyz.xenondevs.nova.util.data.Version
import xyz.xenondevs.nova.util.data.useZip
import kotlin.io.path.notExists
import kotlin.jvm.JvmStatic

@Suppress("unused") // called by generated bootstrap code
internal object AddonBootstrapper {
    
    private val _addons = ArrayList<Addon>()
    val addons: List<Addon>
        get() = _addons
    
    @JvmStatic
    fun createJavaPlugin(context: PluginProviderContext, classLoader: ClassLoader): JavaPlugin {
        checkRequiredNovaVersion(context)
        checkRequiredMinecraftVersion(context)
        
        val mainClass = Class.forName(context.configuration.mainClass, true, classLoader).kotlin
        val obj = mainClass.objectInstance
        
        require(obj is Addon) { "Main class does not implement Addon" }
        require(obj is JavaPlugin) { "Main class does not extend JavaPlugin" }
        
        _addons += obj as Addon
        return obj as JavaPlugin
    }
    
    @JvmStatic
    private fun checkRequiredNovaVersion(context: PluginProviderContext) {
        val novaVersion = LaunchEntryPointHandler.INSTANCE.storage.asSequence()
            .flatMap { (_, storage) -> storage.registeredProviders }
            .first { it.meta.name == "Nova" }
            .meta.version
            .let(::Version)
        
        context.pluginSource.useZip { fs -> 
            val metaPath = fs.resolve("/nova-addon.yml")
            if (metaPath.notExists())
                throw IllegalStateException("Nova addon meta file not found!")
            
            val loader = YamlConfigurationLoader.builder().path(metaPath).build()
            val meta = loader.load()
            
            val requiredNovaVersion = Version(meta.node("nova_version").string!!)
            
            if (novaVersion.compareTo(requiredNovaVersion, 2) != 0)
                throw IllegalArgumentException("Cannot load Nova addon ${context.configuration.displayName} as it requires Nova version " +
                    "$requiredNovaVersion, but the server is running Nova version $novaVersion!")
        }
    }
    
    @JvmStatic
    private fun checkRequiredMinecraftVersion(context: PluginProviderContext) {
        val apiVersion = context.configuration.apiVersion?.let(::Version)
            ?: throw IllegalArgumentException("Missing api version")
        
        if (Version.SERVER_VERSION.compareTo(apiVersion, 2) != 0)
            throw IllegalArgumentException("Cannot load Nova addon ${context.configuration.displayName} as it requires Minecraft version " +
                "$apiVersion, but the server is running Minecraft version ${Version.SERVER_VERSION}!")
    }
    
}