package xyz.xenondevs.nova.config

import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.serialize.TypeSerializerCollection
import org.spongepowered.configurate.yaml.NodeStyle
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.nova.DATA_FOLDER
import xyz.xenondevs.nova.NOVA_JAR
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.addon.AddonBootstrapper
import xyz.xenondevs.nova.addon.id
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.serialization.configurate.NOVA_CONFIGURATE_SERIALIZERS
import xyz.xenondevs.nova.serialization.kotlinx.KeySerializer
import xyz.xenondevs.nova.util.Key
import xyz.xenondevs.nova.util.data.useZip
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.isDirectory
import kotlin.io.path.relativeTo
import kotlin.io.path.walk

private val DEFAULT_CONFIG_ID = Key.key("nova", "config")
private const val DEFAULT_CONFIG_PATH = "configs/config.yml"
val MAIN_CONFIG = Configs[DEFAULT_CONFIG_ID]

@InternalInit(stage = InternalInitStage.PRE_WORLD)
object Configs {
    
    private val customSerializers = HashMap<String, ArrayList<TypeSerializerCollection>>()
    
    private val extractor = ConfigExtractor(
        PermanentStorage.storedValue(
            "stored_configs", 
            MapSerializer(KeySerializer, String.serializer()),
            ::HashMap
        )
    )
    private val configProviders = HashMap<Key, RootConfigProvider>()
    
    private var lastReload = -1L
    
    internal fun extractDefaultConfig() {
        NOVA_JAR.useZip { zip ->
            val from = zip.resolve(DEFAULT_CONFIG_PATH)
            val to = DATA_FOLDER.resolve(DEFAULT_CONFIG_PATH)
            extractConfig(from, to, DEFAULT_CONFIG_ID)
        }
    }
    
    @InitFun
    private fun extractAllConfigs() {
        extractConfigs("nova", NOVA_JAR, DATA_FOLDER)
        for (addon in AddonBootstrapper.addons) {
            extractConfigs(addon.id, addon.file, addon.dataFolder)
        }
        
        lastReload = System.currentTimeMillis()
        configProviders.values.asSequence()
            .filter { it.path.exists() }
            .forEach { it.set(createLoader(it.configId.namespace(), it.path).load()) }
    }
    
    private fun extractConfigs(namespace: String, zipFile: Path, dataFolder: Path) {
        zipFile.useZip { zip ->
            val configsDir = zip.resolve("configs/")
            configsDir.walk()
                .filter { !it.isDirectory() && it.extension.equals("yml", true) }
                .forEach { config ->
                    val relPath = config.relativeTo(configsDir).invariantSeparatorsPathString
                    val configId = Key.key(namespace, relPath.substringBeforeLast('.'))
                    extractConfig(config, dataFolder.resolve("configs").resolve(relPath), configId)
                }
        }
    }
    
    private fun extractConfig(from: Path, to: Path, configId: Key) {
        extractor.extract(configId, from, to)
        val provider = configProviders.getOrPut(configId) { RootConfigProvider(to, configId) }
        provider.reload()
    }
    
    private fun resolveConfigPath(configId: Key): Path {
        val dataFolder = when (configId.namespace()) {
            "nova" -> DATA_FOLDER
            else -> AddonBootstrapper.addons.firstOrNull { it.id == configId.namespace() }?.dataFolder
                ?: throw IllegalArgumentException("No addon with id ${configId.namespace()} found")
        }
        return dataFolder.resolve("configs").resolve(configId.value() + ".yml")
    }
    
    internal fun reload(): List<Key> {
        val reloadedConfigs = configProviders.asSequence()
            .filter { (_, provider) ->
                !provider.path.exists() && provider.fileExisted
                    || provider.path.exists() && provider.path.getLastModifiedTime().toMillis() > lastReload
            } // only reload updated configs
            .onEach { (_, provider) -> provider.reload() }
            .mapTo(ArrayList()) { (id, _) -> id }
        lastReload = System.currentTimeMillis()
        
        Bukkit.getOnlinePlayers().forEach(Player::updateInventory)
        
        return reloadedConfigs
    }
    
    operator fun get(id: String): Provider<CommentedConfigurationNode> =
        get(Key.key(id))
    
    operator fun get(addon: Addon, path: String): Provider<CommentedConfigurationNode> =
        get(Key(addon, path))
    
    operator fun get(id: Key): Provider<CommentedConfigurationNode> =
        configProviders.getOrPut(id) { RootConfigProvider(resolveConfigPath(id), id).also { if (lastReload > -1) it.reload() } }
    
    fun getOrNull(id: String): CommentedConfigurationNode? =
        getOrNull(Key.key(id))
    
    fun getOrNull(id: Key): CommentedConfigurationNode? =
        configProviders[id]?.takeIf { it.loaded }?.get()
    
    fun save(id: String): Unit =
        save(Key.key(id))
    
    fun save(id: Key) {
        val config = getOrNull(id)
            ?: return
        
        createLoader(id.namespace(), resolveConfigPath(id)).save(config)
    }
    
    /**
     * Registers custom [serializers] for configs of [addon].
     */
    fun registerSerializers(addon: Addon, serializers: TypeSerializerCollection) {
        customSerializers.getOrPut(addon.id, ::ArrayList) += serializers
    }
    
    internal fun createBuilder(namespace: String): YamlConfigurationLoader.Builder =
        YamlConfigurationLoader.builder()
            .nodeStyle(NodeStyle.BLOCK)
            .indent(2)
            .defaultOptions { opts ->
                opts.serializers { builder ->
                    builder.registerAll(NOVA_CONFIGURATE_SERIALIZERS)
                    customSerializers[namespace]?.forEach(builder::registerAll)
                }
            }
    
    internal fun createLoader(namespace: String, path: Path): YamlConfigurationLoader =
        createBuilder(namespace).path(path).build()
    
}