package xyz.xenondevs.nova.data.config

import org.bukkit.configuration.file.YamlConfiguration
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.addon.AddonManager
import xyz.xenondevs.nova.addon.AddonsLoader
import xyz.xenondevs.nova.initialize.Initializable
import xyz.xenondevs.nova.material.ItemNovaMaterial
import xyz.xenondevs.nova.util.data.getResourceAsStream
import xyz.xenondevs.nova.util.data.getResources
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

val DEFAULT_CONFIG: YamlConfiguration = NovaConfig["config"]

object NovaConfig : Initializable() {
    
    override val inMainThread = false
    override val dependsOn = setOf(AddonsLoader)
    
    private val configs = HashMap<String, YamlConfiguration>()
    
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
    
    operator fun get(name: String): YamlConfiguration =
        configs[name]!!
    
    operator fun get(material: ItemNovaMaterial): YamlConfiguration =
        configs[material.id.toString()]!!
    
}