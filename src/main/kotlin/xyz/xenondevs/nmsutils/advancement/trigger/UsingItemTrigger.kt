package xyz.xenondevs.nmsutils.advancement.trigger

import xyz.xenondevs.nmsutils.adapter.Adapter
import xyz.xenondevs.nmsutils.advancement.predicate.EntityPredicate
import xyz.xenondevs.nmsutils.advancement.predicate.ItemPredicate
import net.minecraft.advancements.critereon.UsingItemTrigger as MojangUsingItemTrigger

class UsingItemTrigger(
    val player: EntityPredicate?,
    val item: ItemPredicate?
) : Trigger {
    
    companion object : Adapter<UsingItemTrigger, MojangUsingItemTrigger.TriggerInstance> {
        
        override fun toNMS(value: UsingItemTrigger): MojangUsingItemTrigger.TriggerInstance {
            return MojangUsingItemTrigger.TriggerInstance(
                EntityPredicate.EntityPredicateCompositeAdapter.toNMS(value.player),
                ItemPredicate.toNMS(value.item)
            )
        }
        
    }
    
    class Builder : Trigger.Builder<UsingItemTrigger>() {
        
        private var item: ItemPredicate? = null
        
        fun item(init: ItemPredicate.Builder.() -> Unit) {
            item = ItemPredicate.Builder().apply(init).build()
        }
        
        override fun build(): UsingItemTrigger {
            return UsingItemTrigger(player, item)
        }
        
    }
    
}