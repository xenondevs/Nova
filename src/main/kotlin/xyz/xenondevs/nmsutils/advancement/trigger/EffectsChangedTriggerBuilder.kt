package xyz.xenondevs.nmsutils.advancement.trigger

import net.minecraft.advancements.critereon.ContextAwarePredicate
import net.minecraft.advancements.critereon.EffectsChangedTrigger
import net.minecraft.advancements.critereon.MobEffectsPredicate
import xyz.xenondevs.nmsutils.advancement.predicate.EntityPredicateBuilder
import xyz.xenondevs.nmsutils.advancement.predicate.MobEffectsPredicateBuilder
import xyz.xenondevs.nmsutils.internal.util.asContextAwarePredicate

class EffectsChangedTriggerBuilder : TriggerBuilder<EffectsChangedTrigger.TriggerInstance>() {
    
    private var effects = MobEffectsPredicate.ANY
    private var source = ContextAwarePredicate.ANY
    
    fun effect(init: MobEffectsPredicateBuilder.() -> Unit) {
        effects = MobEffectsPredicateBuilder().apply(init).build()
    }
    
    fun source(init: EntityPredicateBuilder.() -> Unit) {
        source = EntityPredicateBuilder().apply(init).build().asContextAwarePredicate()
    }
    
    override fun build(): EffectsChangedTrigger.TriggerInstance {
        return EffectsChangedTrigger.TriggerInstance(player, effects, source)
    }
    
}