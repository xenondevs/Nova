package xyz.xenondevs.nmsutils.advancement.trigger

import xyz.xenondevs.nmsutils.adapter.Adapter
import xyz.xenondevs.nmsutils.advancement.predicate.EntityPredicate
import xyz.xenondevs.nmsutils.advancement.predicate.ItemPredicate
import net.minecraft.advancements.critereon.FilledBucketTrigger as MojangFilledBucketTrigger

class FilledBucketTrigger(
    val player: EntityPredicate?,
    val item: ItemPredicate?
) : Trigger {
    
    companion object : Adapter<FilledBucketTrigger, MojangFilledBucketTrigger.TriggerInstance> {
        
        override fun toNMS(value: FilledBucketTrigger): MojangFilledBucketTrigger.TriggerInstance {
            return MojangFilledBucketTrigger.TriggerInstance(
                EntityPredicate.EntityPredicateCompositeAdapter.toNMS(value.player),
                ItemPredicate.toNMS(value.item)
            )
        }
        
    }
    
    class Builder : Trigger.Builder<FilledBucketTrigger>() {
        
        private var item: ItemPredicate? = null
        
        fun item(init: ItemPredicate.Builder.() -> Unit) {
            item = ItemPredicate.Builder().apply(init).build()
        }
        
        override fun build(): FilledBucketTrigger {
            return FilledBucketTrigger(player, item)
        }
        
    }
    
}