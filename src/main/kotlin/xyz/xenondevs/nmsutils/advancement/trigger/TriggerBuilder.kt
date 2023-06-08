package xyz.xenondevs.nmsutils.advancement.trigger

import net.minecraft.advancements.CriterionTriggerInstance
import net.minecraft.advancements.critereon.ContextAwarePredicate
import xyz.xenondevs.nmsutils.advancement.AdvancementDsl
import xyz.xenondevs.nmsutils.advancement.predicate.EntityPredicateBuilder
import xyz.xenondevs.nmsutils.internal.util.asContextAwarePredicate

@AdvancementDsl
abstract class TriggerBuilder<T : CriterionTriggerInstance> {
    
    protected var player = ContextAwarePredicate.ANY
    
    fun player(init: EntityPredicateBuilder.() -> Unit) {
        player = EntityPredicateBuilder().apply(init).build().asContextAwarePredicate()
    }
    
    internal abstract fun build(): T
    
}