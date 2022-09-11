package xyz.xenondevs.nova.world.loot

import org.bukkit.NamespacedKey
import org.bukkit.entity.EntityType
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.world.LootGenerateEvent
import xyz.xenondevs.nova.initialize.Initializable
import xyz.xenondevs.nova.initialize.InitializationStage
import xyz.xenondevs.nova.util.registerEvents

internal object LootGeneration : Initializable(), Listener {
    
    private val entityKeys by lazy { EntityType.values().associateWith { NamespacedKey.minecraft("entities/${it.name.lowercase()}") } }
    private val lootTables = ArrayList<LootTable>()
    
    override val initializationStage = InitializationStage.POST_WORLD_ASYNC
    override val dependsOn = setOf(LootConfigHandler)
    
    override fun init() {
        registerEvents()
    }
    
    @EventHandler
    private fun handleLootGenerationEvent(event: LootGenerateEvent) {
        lootTables.forEach { loot ->
            if (loot.isAllowed(event.lootTable.key))
                event.loot.addAll(loot.getRandomItems())
        }
    }
    
    @EventHandler
    private fun handleEntityDeath(event: EntityDeathEvent) {
        val key = entityKeys[event.entityType] ?: return
        lootTables.forEach { loot ->
            if (loot.isAllowed(key))
                event.drops.addAll(loot.getRandomItems())
        }
    }
    
    fun register(loot: LootTable) {
        lootTables.add(loot)
    }
    
}