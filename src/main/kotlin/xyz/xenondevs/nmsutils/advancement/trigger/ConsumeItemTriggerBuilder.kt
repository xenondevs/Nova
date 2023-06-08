package xyz.xenondevs.nmsutils.advancement.trigger

import net.minecraft.advancements.critereon.ConsumeItemTrigger
import net.minecraft.advancements.critereon.ItemPredicate
import xyz.xenondevs.nmsutils.advancement.predicate.ItemPredicateBuilder

class ConsumeItemTriggerBuilder : TriggerBuilder<ConsumeItemTrigger.TriggerInstance>() {
    
    private var item = ItemPredicate.ANY
    
    fun item(init: ItemPredicateBuilder.() -> Unit) {
        item = ItemPredicateBuilder().apply(init).build()
    }
    
    override fun build(): ConsumeItemTrigger.TriggerInstance {
        return ConsumeItemTrigger.TriggerInstance(player, item)
    }
    
}