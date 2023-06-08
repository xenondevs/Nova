package xyz.xenondevs.nmsutils.advancement.trigger

import net.minecraft.advancements.critereon.ItemPredicate
import net.minecraft.advancements.critereon.UsedTotemTrigger
import xyz.xenondevs.nmsutils.advancement.predicate.ItemPredicateBuilder

class UsedTotemTriggerBuilder : TriggerBuilder<UsedTotemTrigger.TriggerInstance>() {
    
    private var item = ItemPredicate.ANY
    
    fun item(init: ItemPredicateBuilder.() -> Unit) {
        item = ItemPredicateBuilder().apply(init).build()
    }
    
    override fun build() = UsedTotemTrigger.TriggerInstance(player, item)
    
}