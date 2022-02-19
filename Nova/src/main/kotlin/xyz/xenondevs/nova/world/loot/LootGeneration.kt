package xyz.xenondevs.nova.world.loot

import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.world.LootGenerateEvent
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.addon.loader.AddonsLoader
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.initialize.Initializable
import xyz.xenondevs.nova.util.data.GSON
import xyz.xenondevs.nova.util.data.fromJson
import kotlin.random.Random
import kotlin.random.nextInt

object LootGeneration : Initializable(), Listener {
    
    private val lootTable = ArrayList<LootInfo>()
    
    override val inMainThread = false
    override val dependsOn = setOf(AddonsLoader)
    
    override fun init() {
        lootTable.addAll(GSON.fromJson<ArrayList<LootInfo>>(NovaConfig["loot"].getArray("loot")) ?: emptyList())
        Bukkit.getServer().pluginManager.registerEvents(this, NOVA)
    }
    
    @EventHandler
    fun handleLootGenerationEvent(event: LootGenerateEvent) {
        lootTable.forEach { loot ->
            if (loot.isWhitelisted(event.lootTable.key) && Random.nextInt(1, 100) <= loot.frequency) {
                val amount = Random.nextInt(loot.min..loot.max)
                event.loot.add(loot.item.createItemStack(amount))
            }
        }
    }
    
}