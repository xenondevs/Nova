package xyz.xenondevs.nova.config

import net.minecraft.resources.ResourceLocation
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.yaml.NodeStyle
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.nova.Nova
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.addon.AddonBootstrapper
import xyz.xenondevs.nova.addon.file
import xyz.xenondevs.nova.addon.id
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.serialization.configurate.NOVA_CONFIGURATE_SERIALIZERS
import xyz.xenondevs.nova.util.ResourceLocation
import xyz.xenondevs.nova.util.data.useZip
import java.io.File
import java.nio.file.Path
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.isDirectory
import kotlin.io.path.relativeTo
import kotlin.io.path.walk

private val DEFAULT_CONFIG_ID = ResourceLocation.fromNamespaceAndPath("nova", "config")
private const val DEFAULT_CONFIG_PATH = "configs/config.yml"
val MAIN_CONFIG = Configs[DEFAULT_CONFIG_ID]

@InternalInit(stage = InternalInitStage.PRE_WORLD)
object Configs {
    
    private val extractor = ConfigExtractor(PermanentStorage.storedValue("stored_configs", ::HashMap))
    private val configProviders = HashMap<ResourceLocation, RootConfigProvider>()
    
    private var lastReload = -1L
    
    internal fun extractDefaultConfig() {
        Nova.novaJar.useZip { zip ->
            val from = zip.resolve(DEFAULT_CONFIG_PATH)
            val to = Nova.dataFolder.toPath().resolve(DEFAULT_CONFIG_PATH)
            extractConfig(from, to, DEFAULT_CONFIG_ID)
        }
    }
    
    @InitFun
    private fun extractAllConfigs() {
        extractConfigs("nova", Nova.novaJar, Nova.dataFolder.toPath())
        for (addon in AddonBootstrapper.addons) {
            extractConfigs(addon.id, addon.file, addon.dataFolder.toPath())
        }
        
        lastReload = System.currentTimeMillis()
        configProviders.values.asSequence()
            .filter { it.path.exists() }
            .forEach { it.set(createLoader(it.path).load()) }
    }
    
    private fun extractConfigs(namespace: String, zipFile: File, dataFolder: Path) {
        zipFile.useZip { zip ->
            val configsDir = zip.resolve("configs/")
            configsDir.walk()
                .filter { !it.isDirectory() && it.extension.equals("yml", true) }
                .forEach { config ->
                    val relPath = config.relativeTo(configsDir).invariantSeparatorsPathString
                    val configId = ResourceLocation.fromNamespaceAndPath(namespace, relPath.substringBeforeLast('.'))
                    extractConfig(config, dataFolder.resolve("configs").resolve(relPath), configId)
                }
        }
    }
    
    private fun extractConfig(from: Path, to: Path, configId: ResourceLocation) {
        extractor.extract(configId, from, to)
        val provider = configProviders.getOrPut(configId) { RootConfigProvider(to, configId) }
        provider.set(createLoader(to).load())
    }
    
    private fun resolveConfigPath(configId: ResourceLocation): Path {
        val dataFolder = when(configId.namespace) {
            "nova" -> Nova.dataFolder
            else -> AddonBootstrapper.addons.firstOrNull { it.id == configId.namespace }?.dataFolder
                ?: throw IllegalArgumentException("No addon with id ${configId.namespace} found")
        }.toPath()
        return dataFolder.resolve("configs").resolve(configId.path + ".yml")
    }
    
    internal fun reload(): List<ResourceLocation> {
        val reloadedConfigs = configProviders.asSequence()
            .filter { (_, provider) -> provider.path.exists() }
            .filter { (_, provider) -> provider.path.getLastModifiedTime().toMillis() > lastReload } // only reload updated configs
            .onEach { (_, provider) -> provider.set(createLoader(provider.path).load()) }
            .mapTo(ArrayList()) { (id, _) -> id }
        lastReload = System.currentTimeMillis()
        
        Bukkit.getOnlinePlayers().forEach(Player::updateInventory)
        
        return reloadedConfigs
    }
    
    operator fun get(id: String): Provider<CommentedConfigurationNode> =
        get(ResourceLocation.parse(id))
    
    operator fun get(addon: Addon, path: String): Provider<CommentedConfigurationNode> =
        get(ResourceLocation(addon, path))
    
    operator fun get(id: ResourceLocation): Provider<CommentedConfigurationNode> =
        configProviders.getOrPut(id) { RootConfigProvider(resolveConfigPath(id), id) }
    
    fun getOrNull(id: String): CommentedConfigurationNode? =
        getOrNull(ResourceLocation.parse(id))
    
    fun getOrNull(id: ResourceLocation): CommentedConfigurationNode? =
        configProviders[id]?.takeIf { it.loaded }?.get()
    
    fun save(id: String): Unit =
        save(ResourceLocation.parse(id))
    
    fun save(id: ResourceLocation) {
        val config = getOrNull(id)
            ?: return
        
        createLoader(resolveConfigPath(id)).save(config)
    }
    
    internal fun createBuilder(): YamlConfigurationLoader.Builder =
        YamlConfigurationLoader.builder()
            .nodeStyle(NodeStyle.BLOCK)
            .indent(2)
            .defaultOptions { opts ->
                opts.serializers { builder ->
                    builder.registerAll(NOVA_CONFIGURATE_SERIALIZERS)
                    // TODO: allow addons to register their own serializers
                }
            }
    
    internal fun createLoader(path: Path): YamlConfigurationLoader =
        createBuilder().path(path).build()
    
}