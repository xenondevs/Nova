package xyz.xenondevs.nova.world.loot

import org.bukkit.NamespacedKey
import org.bukkit.entity.EntityType
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.world.LootGenerateEvent
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.util.registerEvents

@InternalInit(
    stage = InternalInitStage.POST_WORLD_ASYNC,
    dependsOn = [LootConfigHandler::class]
)
internal object LootGeneration : Listener {
    
    private val entityKeys by lazy { EntityType.entries.associateWith { NamespacedKey.minecraft("entities/${it.name.lowercase()}") } }
    private val lootTables = ArrayList<LootTable>()
    
    @InitFun
    private fun init() {
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