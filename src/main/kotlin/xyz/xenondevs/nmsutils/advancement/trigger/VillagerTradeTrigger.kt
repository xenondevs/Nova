package xyz.xenondevs.nmsutils.advancement.trigger

import net.minecraft.advancements.critereon.TradeTrigger
import xyz.xenondevs.nmsutils.adapter.Adapter
import xyz.xenondevs.nmsutils.advancement.predicate.EntityPredicate
import xyz.xenondevs.nmsutils.advancement.predicate.ItemPredicate

class VillagerTradeTrigger(
    val player: EntityPredicate?,
    val villager: EntityPredicate?,
    val item: ItemPredicate?
) : Trigger {
    
    companion object : Adapter<VillagerTradeTrigger, TradeTrigger.TriggerInstance> {
        
        override fun toNMS(value: VillagerTradeTrigger): TradeTrigger.TriggerInstance {
            return TradeTrigger.TriggerInstance(
                EntityPredicate.EntityPredicateCompositeAdapter.toNMS(value.player),
                EntityPredicate.EntityPredicateCompositeAdapter.toNMS(value.villager),
                ItemPredicate.toNMS(value.item)
            )
        }
        
    }
    
    class Builder : Trigger.Builder<VillagerTradeTrigger>() {
        
        private var villager: EntityPredicate? = null
        private var item: ItemPredicate? = null
        
        fun villager(init: EntityPredicate.Builder.() -> Unit) {
            villager = EntityPredicate.Builder().apply(init).build()
        }
        
        fun item(init: ItemPredicate.Builder.() -> Unit) {
            item = ItemPredicate.Builder().apply(init).build()
        }
        
        override fun build(): VillagerTradeTrigger {
            return VillagerTradeTrigger(player, villager, item)
        }
        
    }
    
}