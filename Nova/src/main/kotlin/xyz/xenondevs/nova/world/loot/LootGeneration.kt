package xyz.xenondevs.nova.world.loot

import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.entity.EntityType
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.world.LootGenerateEvent
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.initialize.Initializable

internal object LootGeneration : Initializable(), Listener {
    
    private val entityKeys by lazy { EntityType.values().associateWith { NamespacedKey.minecraft("entities/${it.name.lowercase()}") } }
    private val lootTables = ArrayList<LootTable>()
    
    override val inMainThread = false
    override val dependsOn = setOf(LootConfigHandler)
    
    override fun init() {
        Bukkit.getServer().pluginManager.registerEvents(this, NOVA)
    }
    
    @EventHandler
    private fun handleLootGenerationEvent(event: LootGenerateEvent) {
        lootTables.forEach { loot ->
            if (loot.isWhitelisted(event.lootTable.key))
                event.loot.addAll(loot.getRandomItems())
        }
    }
    
    @EventHandler
    private fun handleEntityDeath(event: EntityDeathEvent) {
        val key = entityKeys[event.entityType] ?: return
        lootTables.forEach { loot ->
            if (loot.isWhitelisted(key))
                event.drops.addAll(loot.getRandomItems())
        }
    }
    
    fun register(loot: LootTable) {
        lootTables.add(loot)
    }
    
}