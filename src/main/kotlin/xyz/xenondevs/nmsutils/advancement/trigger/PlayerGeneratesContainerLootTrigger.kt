package xyz.xenondevs.nmsutils.advancement.trigger

import net.minecraft.advancements.critereon.LootTableTrigger
import org.bukkit.NamespacedKey
import xyz.xenondevs.nmsutils.adapter.Adapter
import xyz.xenondevs.nmsutils.advancement.predicate.EntityPredicate
import xyz.xenondevs.nmsutils.util.resourceLocation

class PlayerGeneratesContainerLootTrigger(
    val player: EntityPredicate?,
    val lootTable: String
) : Trigger {
    
    companion object : Adapter<PlayerGeneratesContainerLootTrigger, LootTableTrigger.TriggerInstance> {
        
        override fun toNMS(value: PlayerGeneratesContainerLootTrigger): LootTableTrigger.TriggerInstance {
            return LootTableTrigger.TriggerInstance(
                EntityPredicate.EntityPredicateCompositeAdapter.toNMS(value.player),
                value.lootTable.resourceLocation!!
            )
        }
        
    }
    
    class Builder : Trigger.Builder<PlayerGeneratesContainerLootTrigger>() {
        
        private var lootTable: String? = null
        
        fun lootTable(namespacedKey: NamespacedKey) {
            lootTable = namespacedKey.toString()
        }
        
        fun lootTable(lootTable: String) {
            this.lootTable = lootTable
        }
        
        override fun build(): PlayerGeneratesContainerLootTrigger {
            checkNotNull(lootTable) { "Loot table is not set" }
            return PlayerGeneratesContainerLootTrigger(player, lootTable!!)
        }
        
    }
    
}