package xyz.xenondevs.nmsutils.advancement.trigger

import net.minecraft.advancements.critereon.ItemPredicate
import net.minecraft.advancements.critereon.UsingItemTrigger
import xyz.xenondevs.nmsutils.advancement.predicate.ItemPredicateBuilder

class UsingItemTriggerBuilder : TriggerBuilder<UsingItemTrigger.TriggerInstance>() {
    
    private var item = ItemPredicate.ANY
    
    fun item(init: ItemPredicateBuilder.() -> Unit) {
        item = ItemPredicateBuilder().apply(init).build()
    }
    
    override fun build() = UsingItemTrigger.TriggerInstance(player, item)
    
}