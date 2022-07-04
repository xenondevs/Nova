package xyz.xenondevs.nmsutils.advancement.trigger

import xyz.xenondevs.nmsutils.adapter.Adapter
import xyz.xenondevs.nmsutils.advancement.predicate.EntityPredicate
import xyz.xenondevs.nmsutils.advancement.predicate.ItemPredicate
import net.minecraft.advancements.critereon.UsedTotemTrigger as MojangUsedTotemTrigger

class UsedTotemTrigger(
    val player: EntityPredicate?,
    val item: ItemPredicate?
) : Trigger {
    
    companion object : Adapter<UsedTotemTrigger, MojangUsedTotemTrigger.TriggerInstance> {
        
        override fun toNMS(value: UsedTotemTrigger): MojangUsedTotemTrigger.TriggerInstance {
            return MojangUsedTotemTrigger.TriggerInstance(
                EntityPredicate.EntityPredicateCompositeAdapter.toNMS(value.player),
                ItemPredicate.toNMS(value.item)
            )
        }
        
    }
    
    class Builder : Trigger.Builder<UsedTotemTrigger>() {
        
        private var item: ItemPredicate? = null
        
        fun item(init: ItemPredicate.Builder.() -> Unit) {
            item = ItemPredicate.Builder().apply(init).build()
        }
        
        override fun build(): UsedTotemTrigger {
            return UsedTotemTrigger(player, item)
        }
        
    }
    
}