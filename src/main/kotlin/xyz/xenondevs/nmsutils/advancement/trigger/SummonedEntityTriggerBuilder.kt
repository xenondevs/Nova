package xyz.xenondevs.nmsutils.advancement.trigger

import net.minecraft.advancements.critereon.EntityPredicate
import net.minecraft.advancements.critereon.SummonedEntityTrigger
import xyz.xenondevs.nmsutils.advancement.predicate.EntityPredicateBuilder
import xyz.xenondevs.nmsutils.internal.util.asContextAwarePredicate

class SummonedEntityTriggerBuilder : TriggerBuilder<SummonedEntityTrigger.TriggerInstance>() {
    
    private var entity = EntityPredicate.ANY
    
    fun entity(init: EntityPredicateBuilder.() -> Unit) {
        entity = EntityPredicateBuilder().apply(init).build()
    }
    
    override fun build() = SummonedEntityTrigger.TriggerInstance(player, entity.asContextAwarePredicate())
    
}