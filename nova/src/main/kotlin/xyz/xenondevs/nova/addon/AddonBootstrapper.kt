@file:Suppress("UnstableApiUsage")

package xyz.xenondevs.nova.addon;

import io.papermc.paper.plugin.bootstrap.BootstrapContext
import io.papermc.paper.plugin.bootstrap.PluginProviderContext
import io.papermc.paper.plugin.entrypoint.LaunchEntryPointHandler
import org.bukkit.plugin.java.JavaPlugin
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import xyz.xenondevs.commons.version.Version
import xyz.xenondevs.nova.BOOTSTRAPPER
import xyz.xenondevs.nova.util.SERVER_VERSION
import xyz.xenondevs.nova.util.data.useZip
import kotlin.io.path.Path
import kotlin.io.path.notExists

@Suppress("unused") // called by generated bootstrap code
internal object AddonBootstrapper {
    
    private val _addons = ArrayList<Addon>()
    
    @JvmStatic
    val addons: List<Addon>
        get() = _addons
    
    @JvmStatic
    fun bootstrap(context: BootstrapContext, classLoader: ClassLoader) {
        val addonMeta = readAddonMeta(context)
        
        checkRequiredNovaVersion(context, addonMeta)
        checkRequiredMinecraftVersion(context)
        
        val addon = getAddonInstance(addonMeta, classLoader)
        addon.pluginMeta = context.pluginMeta
        addon.file = context.pluginSource
        addon.dataFolder = Path("plugins", context.pluginMeta.name)
        addon.logger = context.logger
        
        _addons += addon
        BOOTSTRAPPER.handleAddonBootstrap(context)
    }
    
    @JvmStatic
    fun handleJavaPluginCreated(plugin: JavaPlugin, context: PluginProviderContext, classLoader: ClassLoader) {
        val addonMeta = readAddonMeta(context)
        getAddonInstance(addonMeta, classLoader).plugin = plugin
    }
    
    private fun readAddonMeta(context: PluginProviderContext): ConfigurationNode {
        context.pluginSource.useZip { fs ->
            val metaPath = fs.resolve("/nova-addon.yml")
            if (metaPath.notExists())
                throw IllegalStateException("Nova addon meta file not found!")
            
            val loader = YamlConfigurationLoader.builder().path(metaPath).build()
            return loader.load()
        }
    }
    
    private fun getAddonInstance(addonMeta: ConfigurationNode, classLoader: ClassLoader): Addon {
        val mainClass = Class.forName(
            addonMeta.node("main").string ?: throw NoSuchElementException("Missing entry 'main' in nova-addon.yml"),
            true,
            classLoader
        ).kotlin
        val addon = mainClass.objectInstance
        require(addon is Addon) { "Main class does not extend Addon" }
        return addon
    }
    
    private fun checkRequiredNovaVersion(context: PluginProviderContext, addonMeta: ConfigurationNode) {
        val novaVersion = LaunchEntryPointHandler.INSTANCE.storage.asSequence()
            .flatMap { (_, storage) -> storage.registeredProviders }
            .first { it.meta.name == "Nova" }
            .meta.version
            .let(::Version)
        
        val requiredNovaVersion = Version(
            addonMeta.node("nova_version").string ?: throw NoSuchElementException("Missing entry 'nova_version' in nova-addon.yml")
        )
        
        if (novaVersion.compareTo(requiredNovaVersion, 2) != 0)
            throw IllegalArgumentException("Cannot load Nova addon ${context.configuration.displayName} as it requires Nova version " +
                "$requiredNovaVersion, but the server is running Nova version $novaVersion!")
    }
    
    private fun checkRequiredMinecraftVersion(context: PluginProviderContext) {
        val apiVersion = context.configuration.apiVersion?.let(::Version)
            ?: throw IllegalArgumentException("Missing api version")
        
        if (SERVER_VERSION.compareTo(apiVersion, 2) != 0)
            throw IllegalArgumentException("Cannot load Nova addon ${context.configuration.displayName} as it requires Minecraft version " +
                "$apiVersion, but the server is running Minecraft version $SERVER_VERSION!")
    }
    
}