package xyz.xenondevs.nmsutils.advancement.trigger

import net.minecraft.advancements.critereon.EntityPredicate
import net.minecraft.advancements.critereon.TameAnimalTrigger
import xyz.xenondevs.nmsutils.advancement.predicate.EntityPredicateBuilder
import xyz.xenondevs.nmsutils.internal.util.asContextAwarePredicate

class TameAnimalTriggerBuilder : TriggerBuilder<TameAnimalTrigger.TriggerInstance>() {
    
    private var entity = EntityPredicate.ANY
    
    fun entity(init: EntityPredicateBuilder.() -> Unit) {
        entity = EntityPredicateBuilder().apply(init).build()
    }
    
    override fun build() = TameAnimalTrigger.TriggerInstance(player, entity.asContextAwarePredicate())
    
}