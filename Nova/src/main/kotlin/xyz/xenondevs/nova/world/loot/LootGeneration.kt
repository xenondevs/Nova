package xyz.xenondevs.nova.world.loot

import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.entity.EntityType
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.world.LootGenerateEvent
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.addon.AddonsInitializer
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.initialize.Initializable
import kotlin.random.Random

object LootGeneration : Initializable(), Listener {
    
    private val entityKeys by lazy { EntityType.values().associateWith { NamespacedKey.minecraft("entities/${it.name.lowercase()}") } }
    private val lootTable = ArrayList<LootInfo>()
    
    override val inMainThread = false
    override val dependsOn = setOf(NovaConfig, AddonsInitializer)
    
    override fun init() {
        Bukkit.getServer().pluginManager.registerEvents(this, NOVA)
    }
    
    @EventHandler
    fun handleLootGenerationEvent(event: LootGenerateEvent) {
        lootTable.forEach { loot ->
            if (loot.isWhitelisted(event.lootTable.key) && Random.nextInt(1, 100) <= loot.frequency) {
                event.loot.add(loot.item.setAmount(loot.getRandomAmount()).get())
            }
        }
    }
    
    @EventHandler
    fun handleEntityDeath(event: EntityDeathEvent) {
        val key = entityKeys[event.entityType] ?: return
        lootTable.forEach { loot ->
            if (loot.isWhitelisted(key) && Random.nextInt(1, 100) <= loot.frequency) {
                event.drops.add(loot.item.setAmount(loot.getRandomAmount()).get())
            }
        }
    }
    
    fun register(loot: LootInfo) {
        lootTable.add(loot)
    }
    
    @LootDsl
    fun register(builder: LootInfo.Builder.() -> Unit) {
        lootTable.add(LootInfo.Builder().apply(builder).build())
    }
}