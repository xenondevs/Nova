package xyz.xenondevs.nova.data.config

import org.bukkit.configuration.file.YamlConfiguration
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.UpdateReminder
import xyz.xenondevs.nova.addon.AddonManager
import xyz.xenondevs.nova.addon.AddonsLoader
import xyz.xenondevs.nova.data.resources.upload.AutoUploadManager
import xyz.xenondevs.nova.initialize.Initializable
import xyz.xenondevs.nova.material.ItemNovaMaterial
import xyz.xenondevs.nova.player.ability.AbilityManager
import xyz.xenondevs.nova.tileentity.TileEntityManager
import xyz.xenondevs.nova.tileentity.network.NetworkManager
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeTypeRegistry
import xyz.xenondevs.nova.ui.overlay.ActionbarOverlayManager
import xyz.xenondevs.nova.util.data.getResourceAsStream
import xyz.xenondevs.nova.util.data.getResources
import xyz.xenondevs.nova.world.ChunkReloadWatcher
import xyz.xenondevs.nova.world.block.limits.TileEntityLimits
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set
import kotlin.reflect.KProperty

val DEFAULT_CONFIG by configReloadable { NovaConfig["config"] }

object NovaConfig : Initializable() {
    
    override val inMainThread = false
    override val dependsOn = setOf(AddonsLoader)
    
    private val configs = HashMap<String, YamlConfiguration>()
    internal val configReloadables = arrayListOf<Reloadable>()
    
    fun loadDefaultConfig() {
        LOGGER.info("Loading default config")
        
        configs["config"] = ConfigExtractor.extract(
            "configs/config.yml",
            getResourceAsStream("configs/config.yml")!!.readAllBytes()
        )
    }
    
    override fun init() {
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
                        loader.classLoader.getResourceAsStream(it)!!.readAllBytes()
                    )
                }
        }
    }
    
    internal fun reload() {
        loadDefaultConfig()
        init()
        UpgradeTypeRegistry.types.forEach(Reloadable::reload)
        configReloadables.sorted().forEach(Reloadable::reload)
        TileEntityManager.tileEntities.forEach(Reloadable::reload)
        NetworkManager.queueAsync { it.networks.forEach(Reloadable::reload) }
        AbilityManager.activeAbilities.values.flatMap { it.values }.forEach(Reloadable::reload)
        AutoUploadManager.reload()
        ActionbarOverlayManager.reload()
        TileEntityLimits.reload()
        ChunkReloadWatcher.reload()
        UpdateReminder.reload()
    }
    
    operator fun get(name: String): YamlConfiguration =
        configs[name]!!
    
    operator fun get(material: ItemNovaMaterial): YamlConfiguration =
        configs[material.id.toString()]!!
    
}

fun <T : Any> configReloadable(initializer: () -> T): ConfigReloadable<T> {
    return ConfigReloadable(initializer)
}

fun <T : Any> notReloadable(value: T): StaticReloadable<T> = StaticReloadable(value)

class ConfigReloadable<T : Any> internal constructor(val initializer: () -> T) : ValueReloadable<T> {
    
    private var shouldReload = true
    private var _value: T? = null
    override val value: T
        get() {
            if (shouldReload) {
                _value = initializer()
                shouldReload = false
            }
            
            return _value!!
        }
    
    init {
        NovaConfig.configReloadables += this
    }
    
    override fun reload() {
        shouldReload = true
    }
    
}

class StaticReloadable<T : Any> internal constructor(override val value: T) : ValueReloadable<T>

interface ValueReloadable<T : Any> : Reloadable {
    
    val value: T
    
    operator fun getValue(ref: Any?, property: KProperty<*>): T = value
    
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