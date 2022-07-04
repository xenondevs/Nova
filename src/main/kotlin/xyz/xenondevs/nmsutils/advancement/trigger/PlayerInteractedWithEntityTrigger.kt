package xyz.xenondevs.nmsutils.advancement.trigger

import net.minecraft.advancements.critereon.PlayerInteractTrigger
import xyz.xenondevs.nmsutils.adapter.Adapter
import xyz.xenondevs.nmsutils.advancement.predicate.EntityPredicate
import xyz.xenondevs.nmsutils.advancement.predicate.ItemPredicate

class PlayerInteractedWithEntityTrigger(
    val player: EntityPredicate?,
    val item: ItemPredicate?,
    val entity: EntityPredicate?
) : Trigger {
    
    companion object : Adapter<PlayerInteractedWithEntityTrigger, PlayerInteractTrigger.TriggerInstance> {
        
        override fun toNMS(value: PlayerInteractedWithEntityTrigger): PlayerInteractTrigger.TriggerInstance {
            return PlayerInteractTrigger.TriggerInstance(
                EntityPredicate.EntityPredicateCompositeAdapter.toNMS(value.player),
                ItemPredicate.toNMS(value.item),
                EntityPredicate.EntityPredicateCompositeAdapter.toNMS(value.entity)
            )
        }
        
    }
    
    class Builder : Trigger.Builder<PlayerInteractedWithEntityTrigger>() {
        
        private var item: ItemPredicate? = null
        private var entity: EntityPredicate? = null
        
        fun item(init: ItemPredicate.Builder.() -> Unit) {
            item = ItemPredicate.Builder().apply(init).build()
        }
        
        fun entity(init: EntityPredicate.Builder.() -> Unit) {
            entity = EntityPredicate.Builder().apply(init).build()
        }
        
        override fun build(): PlayerInteractedWithEntityTrigger {
            return PlayerInteractedWithEntityTrigger(player, item, entity)
        }
        
    }
    
}