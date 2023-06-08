package xyz.xenondevs.nmsutils.advancement.trigger

import net.minecraft.advancements.critereon.BredAnimalsTrigger
import net.minecraft.advancements.critereon.ContextAwarePredicate
import xyz.xenondevs.nmsutils.advancement.predicate.EntityPredicateBuilder
import xyz.xenondevs.nmsutils.internal.util.asContextAwarePredicate

class BredAnimalsTriggerBuilder : TriggerBuilder<BredAnimalsTrigger.TriggerInstance>() {
    
    private var parent = ContextAwarePredicate.ANY
    private var partner = ContextAwarePredicate.ANY
    private var child = ContextAwarePredicate.ANY
    
    fun parent(init: EntityPredicateBuilder.() -> Unit) {
        parent = EntityPredicateBuilder().apply(init).build().asContextAwarePredicate()
    }
    
    fun partner(init: EntityPredicateBuilder.() -> Unit) {
        partner = EntityPredicateBuilder().apply(init).build().asContextAwarePredicate()
    }
    
    fun child(init: EntityPredicateBuilder.() -> Unit) {
        child = EntityPredicateBuilder().apply(init).build().asContextAwarePredicate()
    }
    
    override fun build(): BredAnimalsTrigger.TriggerInstance {
        return BredAnimalsTrigger.TriggerInstance(player, parent, partner, child)
    }
    
}