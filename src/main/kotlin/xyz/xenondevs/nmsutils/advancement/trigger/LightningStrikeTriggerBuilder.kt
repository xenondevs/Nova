package xyz.xenondevs.nmsutils.advancement.trigger

import net.minecraft.advancements.critereon.ContextAwarePredicate
import net.minecraft.advancements.critereon.LightningStrikeTrigger
import xyz.xenondevs.nmsutils.advancement.predicate.EntityPredicateBuilder
import xyz.xenondevs.nmsutils.internal.util.asContextAwarePredicate

class LightningStrikeTriggerBuilder : TriggerBuilder<LightningStrikeTrigger.TriggerInstance>() {
    
    private var lightning = ContextAwarePredicate.ANY
    private var bystander = ContextAwarePredicate.ANY
    
    fun lightning(init: EntityPredicateBuilder.() -> Unit) {
        lightning = EntityPredicateBuilder().apply(init).build().asContextAwarePredicate()
    }
    
    fun bystander(init: EntityPredicateBuilder.() -> Unit) {
        bystander = EntityPredicateBuilder().apply(init).build().asContextAwarePredicate()
    }
    
    override fun build(): LightningStrikeTrigger.TriggerInstance {
        return LightningStrikeTrigger.TriggerInstance(player, lightning, bystander)
    }
    
}