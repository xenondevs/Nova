package xyz.xenondevs.nova.player.advancement

import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import xyz.xenondevs.nova.IS_VERSION_CHANGE
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.initialize.Initializable
import xyz.xenondevs.nova.integration.customitems.CustomItemServiceManager
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import xyz.xenondevs.nova.util.awardAdvancement
import xyz.xenondevs.nova.util.data.getResourceAsStream
import xyz.xenondevs.nova.util.data.getResources
import xyz.xenondevs.nova.util.insert
import xyz.xenondevs.nova.util.minecraftServer
import java.io.File

private val ITEM_REGEXPS = listOf(
    Regex("""("item":\s*")(nova:[a-z0-9._-]+)(")"""),
    Regex("""("items":\s*\[\s*")(nova:[a-z0-9._-]+)("\s*])""")
)

object AdvancementManager : Initializable(), Listener {
    
    override val inMainThread = true
    override val dependsOn = CustomItemServiceManager
    
    override fun init() {
        Bukkit.getServer().pluginManager.registerEvents(this, NOVA)
        LOGGER.info("Loading advancements")
        
        val novaDataPack = File("world/datapacks/nova")
        if (IS_VERSION_CHANGE) {
            File("world/datapacks/bukkit/data/nova").deleteRecursively()
            novaDataPack.deleteRecursively()
        }
        getResources("datapack/").forEach { name ->
            var content = getResourceAsStream(name).use { it!!.readAllBytes() }.decodeToString()
            
            try {
                for (regex in ITEM_REGEXPS) {
                    var result: MatchResult? = null
                    while (regex.find(content)?.let { result = it } != null) {
                        val materialMatch = result!!.groups[2]!!
                        val material = NovaMaterialRegistry.get(materialMatch.value.removePrefix("nova:"))
                        
                        content = content
                            .insert(result!!.groups[3]!!.range.last + 1, """ ,"nbt": "{CustomModelData:${material.item.data}}" """)
                            .replaceRange(materialMatch.range, "minecraft:${material.item.material.name.lowercase()}")
                    }
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            
            val file = File(novaDataPack, name.substringAfter("datapack/")).apply { parentFile.mkdirs() }
            file.writeText(content)
        }
        
        minecraftServer.reloadResources(listOf("file/nova"))
    }
    
    @EventHandler
    fun handlePlayerJoin(event: PlayerJoinEvent) {
        event.player.awardAdvancement(NamespacedKey(NOVA, "root"))
    }
    
}