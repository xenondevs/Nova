package xyz.xenondevs.nmsutils.advancement.trigger

import net.minecraft.advancements.critereon.FilledBucketTrigger
import net.minecraft.advancements.critereon.ItemPredicate
import xyz.xenondevs.nmsutils.advancement.predicate.ItemPredicateBuilder

class FilledBucketTriggerBuilder : TriggerBuilder<FilledBucketTrigger.TriggerInstance>() {
    
    private var item = ItemPredicate.ANY
    
    fun item(init: ItemPredicateBuilder.() -> Unit) {
        item = ItemPredicateBuilder().apply(init).build()
    }
    
    override fun build(): FilledBucketTrigger.TriggerInstance {
        return FilledBucketTrigger.TriggerInstance(player, item)
    }
    
}