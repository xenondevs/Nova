package xyz.xenondevs.nmsutils.advancement.trigger

import xyz.xenondevs.nmsutils.adapter.Adapter
import xyz.xenondevs.nmsutils.advancement.predicate.EffectPredicate
import xyz.xenondevs.nmsutils.advancement.predicate.EntityPredicate
import net.minecraft.advancements.critereon.EffectsChangedTrigger as MojangEffectsChangedTrigger

class EffectsChangedTrigger(
    val player: EntityPredicate?,
    val effects: List<EffectPredicate>?,
    val source: EntityPredicate?
) : Trigger {
    
    companion object : Adapter<EffectsChangedTrigger, MojangEffectsChangedTrigger.TriggerInstance> {
        
        override fun toNMS(value: EffectsChangedTrigger): MojangEffectsChangedTrigger.TriggerInstance {
            return MojangEffectsChangedTrigger.TriggerInstance(
                EntityPredicate.EntityPredicateCompositeAdapter.toNMS(value.player),
                EffectPredicate.toNMS(value.effects),
                EntityPredicate.EntityPredicateCompositeAdapter.toNMS(value.source)
            )
        }
        
    }
    
    class Builder : Trigger.Builder<EffectsChangedTrigger>() {
        
        private val effects = ArrayList<EffectPredicate>()
        private var source: EntityPredicate? = null
        
        fun effect(init: EffectPredicate.Builder.() -> Unit) {
            effects += EffectPredicate.Builder().apply(init).build()
        }
        
        fun source(init: EntityPredicate.Builder.() -> Unit) {
            source = EntityPredicate.Builder().apply(init).build()
        }
        
        override fun build(): EffectsChangedTrigger {
            return EffectsChangedTrigger(player, effects.takeUnless(List<*>::isEmpty), source)
        }
        
    }
    
}