package xyz.xenondevs.nova.data.config

import net.minecraft.resources.ResourceLocation
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.yaml.NodeStyle
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.addon.AddonManager
import xyz.xenondevs.nova.addon.AddonsLoader
import xyz.xenondevs.nova.data.resources.upload.AutoUploadManager
import xyz.xenondevs.nova.data.serialization.configurate.NOVA_CONFIGURATE_SERIALIZERS
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.item.ItemCategories
import xyz.xenondevs.nova.player.ability.AbilityManager
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.tileentity.TileEntityManager
import xyz.xenondevs.nova.tileentity.network.NetworkManager
import xyz.xenondevs.nova.ui.overlay.actionbar.ActionbarOverlayManager
import xyz.xenondevs.nova.ui.overlay.bossbar.BossBarOverlayManager
import xyz.xenondevs.nova.ui.waila.WailaManager
import xyz.xenondevs.nova.update.UpdateReminder
import xyz.xenondevs.nova.util.data.useZip
import xyz.xenondevs.nova.world.ChunkReloadWatcher
import java.io.File
import java.nio.file.Path
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set
import kotlin.io.path.extension
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.isDirectory
import kotlin.io.path.relativeTo
import kotlin.io.path.walk

private const val DEFAULT_CONFIG_NAME = "config"
private const val DEFAULT_CONFIG_PATH = "configs/config.yml"

val MAIN_CONFIG by lazy { Configs[DEFAULT_CONFIG_NAME] }

@InternalInit(
    stage = InternalInitStage.PRE_WORLD,
    dependsOn = [AddonsLoader::class]
)
object Configs {
    
    private val configProviders = HashMap<String, ConfigProvider>()
    internal val reloadables = arrayListOf<Reloadable>()
    
    private var mainLoaded = false
    private var loaded = false
    
    internal fun extractDefaultConfig() {
        LOGGER.info("Extracting default config")
        NOVA.novaJar.useZip { extractConfig(it.resolve(DEFAULT_CONFIG_PATH), DEFAULT_CONFIG_NAME, DEFAULT_CONFIG_PATH, ::mainLoaded) }
        ConfigExtractor.saveStoredConfigs()
        mainLoaded = true
    }
    
    @InitFun
    private fun extractAllConfigs() {
        LOGGER.info("Extracting configs")
        
        extractConfigs("nova", NOVA.novaJar, "configs/nova/")
        for ((id, loader) in AddonManager.loaders) {
            extractConfigs(id, loader.file, "configs/")
        }
        
        ConfigExtractor.saveStoredConfigs()
        loaded = true
        
        configProviders.values.forEach(ConfigProvider::update)
    }
    
    private fun extractConfigs(namespace: String, zipFile: File, configsPath: String) {
        zipFile.useZip { zip ->
            val configsDir = zip.resolve(configsPath)
            configsDir.walk()
                .filter { !it.isDirectory() && it.extension.equals("yml", true) }
                .forEach { config ->
                    val relPath = config.relativeTo(configsDir).invariantSeparatorsPathString
                    val configName = "$namespace:${relPath.substringBeforeLast('.')}"
                    val configPath = "configs/$namespace/$relPath"
                    extractConfig(config, configName, configPath, ::loaded)
                }
        }
    }
    
    private fun extractConfig(config: Path, configId: String, configPath: String, loadValidation: () -> Boolean) {
        val destFile = File(NOVA.dataFolder, configPath).toPath()
        ConfigExtractor.extract(configPath, destFile, config)
        if (configId !in configProviders)
            configProviders[configId] = ConfigProvider(destFile, configPath, loadValidation)
    }
    
    private fun createConfigProvider(configId: String, loadValidation: () -> Boolean): ConfigProvider {
        val (namespace, path) = configId.split(':')
        val relPath = "configs/$namespace/$path.yml"
        val file = File(NOVA.dataFolder, relPath).toPath()
        return ConfigProvider(file, relPath, loadValidation)
    }
    
    internal fun reload() {
        extractAllConfigs()
        reloadables.sorted().forEach(Reloadable::reload)
        NovaRegistries.ITEM.forEach(Reloadable::reload)
        TileEntityManager.tileEntities.forEach(Reloadable::reload)
        NetworkManager.queueAsync { it.networks.forEach(Reloadable::reload) }
        AbilityManager.activeAbilities.values.flatMap { it.values }.forEach(Reloadable::reload)
        AutoUploadManager.reload()
        ActionbarOverlayManager.reload()
        BossBarOverlayManager.reload()
        ChunkReloadWatcher.reload()
        UpdateReminder.reload()
        ItemCategories.reload()
        WailaManager.reload()
        Bukkit.getOnlinePlayers().forEach(Player::updateInventory)
    }
    
    operator fun get(id: ResourceLocation): ConfigProvider =
        get(id.toString())
    
    operator fun get(id: String): ConfigProvider =
        configProviders.getOrPut(id) { createConfigProvider(id, ::loaded) }
    
    fun getProviderOrNull(id: ResourceLocation): ConfigProvider? =
        getProviderOrNull(id.toString())
    
    fun getProviderOrNull(id: String): ConfigProvider? =
        configProviders[id]
    
    fun getOrNull(id: ResourceLocation): CommentedConfigurationNode? =
        getOrNull(id.toString())
    
    fun getOrNull(id: String): CommentedConfigurationNode? =
        configProviders[id]?.value
    
    fun save(id: ResourceLocation) =
        save(id.toString())
    
    fun save(id: String) {
        createLoader(File(NOVA.dataFolder, "configs/$id.yml")).save(get(id).value)
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
    
    internal fun createLoader(file: File): YamlConfigurationLoader =
        createLoader(file.toPath())
    
    internal fun createLoader(path: Path): YamlConfigurationLoader =
        createBuilder().path(path).build()
    
}