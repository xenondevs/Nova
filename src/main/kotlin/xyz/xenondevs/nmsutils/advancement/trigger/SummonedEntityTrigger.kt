package xyz.xenondevs.nmsutils.advancement.trigger

import xyz.xenondevs.nmsutils.adapter.Adapter
import xyz.xenondevs.nmsutils.advancement.predicate.EntityPredicate
import net.minecraft.advancements.critereon.SummonedEntityTrigger as MojangSummonedEntityTrigger

class SummonedEntityTrigger(
    val player: EntityPredicate?,
    val entity: EntityPredicate?
) : Trigger {
    
    companion object : Adapter<SummonedEntityTrigger, MojangSummonedEntityTrigger.TriggerInstance> {
        
        override fun toNMS(value: SummonedEntityTrigger): MojangSummonedEntityTrigger.TriggerInstance {
            return MojangSummonedEntityTrigger.TriggerInstance(
                EntityPredicate.EntityPredicateCompositeAdapter.toNMS(value.player),
                EntityPredicate.EntityPredicateCompositeAdapter.toNMS(value.entity)
            )
        }
        
    }
    
    class Builder : Trigger.Builder<SummonedEntityTrigger>() {
        
        private var entity: EntityPredicate? = null
        
        fun entity(init: EntityPredicate.Builder.() -> Unit) {
            entity = EntityPredicate.Builder().apply(init).build()
        }
        
        override fun build(): SummonedEntityTrigger {
            return SummonedEntityTrigger(player, entity)
        }
        
    }
    
}