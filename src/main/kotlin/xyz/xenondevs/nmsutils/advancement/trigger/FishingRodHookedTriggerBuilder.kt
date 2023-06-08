package xyz.xenondevs.nmsutils.advancement.trigger

import net.minecraft.advancements.critereon.ContextAwarePredicate
import net.minecraft.advancements.critereon.FishingRodHookedTrigger
import net.minecraft.advancements.critereon.ItemPredicate
import xyz.xenondevs.nmsutils.advancement.predicate.EntityPredicateBuilder
import xyz.xenondevs.nmsutils.advancement.predicate.ItemPredicateBuilder
import xyz.xenondevs.nmsutils.internal.util.asContextAwarePredicate

class FishingRodHookedTriggerBuilder : TriggerBuilder<FishingRodHookedTrigger.TriggerInstance>() {
    
    private var entity = ContextAwarePredicate.ANY
    private var item = ItemPredicate.ANY
    private var rod = ItemPredicate.ANY
    
    fun entity(init: EntityPredicateBuilder.() -> Unit) {
        entity = EntityPredicateBuilder().apply(init).build().asContextAwarePredicate()
    }
    
    fun item(init: ItemPredicateBuilder.() -> Unit) {
        item = ItemPredicateBuilder().apply(init).build()
    }
    
    fun rod(init: ItemPredicateBuilder.() -> Unit) {
        rod = ItemPredicateBuilder().apply(init).build()
    }
    
    override fun build(): FishingRodHookedTrigger.TriggerInstance {
        return FishingRodHookedTrigger.TriggerInstance(player, rod, entity, item)
    }
    
}