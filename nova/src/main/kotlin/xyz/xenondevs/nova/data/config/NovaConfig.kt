package xyz.xenondevs.nova.data.config

import org.bukkit.Bukkit
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.immutable.provider
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.UpdateReminder
import xyz.xenondevs.nova.addon.AddonManager
import xyz.xenondevs.nova.addon.AddonsLoader
import xyz.xenondevs.nova.data.resources.upload.AutoUploadManager
import xyz.xenondevs.nova.initialize.InitializationStage
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.material.ItemCategories
import xyz.xenondevs.nova.material.ItemNovaMaterial
import xyz.xenondevs.nova.player.PlayerFreezer
import xyz.xenondevs.nova.player.ability.AbilityManager
import xyz.xenondevs.nova.tileentity.TileEntityManager
import xyz.xenondevs.nova.tileentity.network.NetworkManager
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeTypeRegistry
import xyz.xenondevs.nova.ui.overlay.actionbar.ActionbarOverlayManager
import xyz.xenondevs.nova.ui.overlay.bossbar.BossBarOverlayManager
import xyz.xenondevs.nova.util.data.getResourceAsStream
import xyz.xenondevs.nova.util.data.getResources
import xyz.xenondevs.nova.world.ChunkReloadWatcher
import xyz.xenondevs.nova.world.block.limits.TileEntityLimits
import java.io.File
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set
import kotlin.reflect.KProperty

val DEFAULT_CONFIG by configReloadable { NovaConfig["config"] }

@InternalInit(
    stage = InitializationStage.PRE_WORLD,
    dependsOn = [AddonsLoader::class]
)
object NovaConfig {
    
    private val configs = HashMap<String, YamlConfiguration>()
    internal val reloadables = arrayListOf<Reloadable>()
    
    internal fun loadDefaultConfig() {
        LOGGER.info("Loading default config")
        
        configs["config"] = ConfigExtractor.extract(
            "configs/config.yml",
            getResourceAsStream("configs/config.yml")!!.readAllBytes()
        )
    }
    
    fun init() {
        LOGGER.info("Loading configs")
        
        getResources("configs/nova/")
            .filter { it.endsWith(".yml", true) }
            .forEach {
                val path = it.substringAfter("configs/nova/")
                val configName = "nova:${path.substringBeforeLast('.')}"
                configs[configName] = ConfigExtractor.extract(
                    "configs/nova/$path",
                    getResourceAsStream(it)!!.readAllBytes()
                )
            }
        
        AddonManager.loaders.forEach { (id, loader) ->
            getResources(loader.file, "configs/")
                .filter { it.endsWith(".yml", true) }
                .forEach {
                    val path = it.substringAfter("configs/")
                    val configName = "$id:${path.substringBeforeLast('.')}"
                    configs[configName] = ConfigExtractor.extract(
                        "configs/$id/$path",
                        getResourceAsStream(loader.file, it)!!.readAllBytes()
                    )
                }
        }
        
        ConfigExtractor.saveStoredConfigs()
    }
    
    internal fun reload() {
        loadDefaultConfig()
        init()
        UpgradeTypeRegistry.types.forEach(Reloadable::reload)
        reloadables.sorted().forEach(Reloadable::reload)
        TileEntityManager.tileEntities.forEach(Reloadable::reload)
        NetworkManager.queueAsync { it.networks.forEach(Reloadable::reload) }
        AbilityManager.activeAbilities.values.flatMap { it.values }.forEach(Reloadable::reload)
        AutoUploadManager.reload()
        ActionbarOverlayManager.reload()
        BossBarOverlayManager.reload()
        TileEntityLimits.reload()
        ChunkReloadWatcher.reload()
        UpdateReminder.reload()
        PlayerFreezer.reload()
        ItemCategories.reload()
        Bukkit.getOnlinePlayers().forEach(Player::updateInventory)
    }
    
    operator fun get(name: String): YamlConfiguration =
        configs[name] ?: throw IllegalArgumentException("Config not found: $name")
    
    operator fun get(material: ItemNovaMaterial): YamlConfiguration =
        configs[material.id.toString()] ?: throw IllegalArgumentException("Config not found: ${material.id}")
    
    fun getOrNull(name: String): YamlConfiguration? =
        configs[name]
    
    fun getOrNull(material: ItemNovaMaterial): YamlConfiguration? =
        configs[material.id.toString()]
    
    fun save(name: String) {
        configs[name]!!.save(File(NOVA.dataFolder, "configs/$name.yml"))
    }
    
}

interface Reloadable : Comparable<Reloadable> {
    
    fun reload() = Unit
    
    override fun compareTo(other: Reloadable): Int =
        when {
            other !is ConfigReloadable<*> -> -1
            this is ConfigReloadable<*> -> 0
            else -> 1
        }
    
}

//<editor-fold desc="config reloadable", defaultstate="collapsed">
fun <T : Any> configReloadable(initializer: () -> T): Provider<T> = ConfigReloadable(initializer)

@Deprecated("Replaced by Provider", ReplaceWith("provider(value)", "xyz.xenondevs.nova.data.provider.provider"))
fun <T : Any> notReloadable(value: T): Provider<T> = provider(value)

@Suppress("DEPRECATION")
private class ConfigReloadable<T : Any>(val initializer: () -> T) : Provider<T>(), ValueReloadable<T> {
    
    init {
        NovaConfig.reloadables += this
    }
    
    override fun loadValue(): T = initializer()
    override fun reload() = update()
    
}

@Deprecated("Replaced by Provider", ReplaceWith("Provider<T>", "xyz.xenondevs.nova.data.provider.Provider"))
interface ValueReloadable<T : Any> : Reloadable {
    val value: T
    operator fun getValue(thisRef: Any?, property: KProperty<*>?): T
}
//</editor-fold>