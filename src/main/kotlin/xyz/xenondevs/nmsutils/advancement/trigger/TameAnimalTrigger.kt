package xyz.xenondevs.nmsutils.advancement.trigger

import xyz.xenondevs.nmsutils.adapter.Adapter
import xyz.xenondevs.nmsutils.advancement.predicate.EntityPredicate
import net.minecraft.advancements.critereon.TameAnimalTrigger as MojangTameAnimalTrigger

class TameAnimalTrigger(
    val player: EntityPredicate?,
    val entity: EntityPredicate?
) : Trigger {
    
    companion object : Adapter<TameAnimalTrigger, MojangTameAnimalTrigger.TriggerInstance> {
        
        override fun toNMS(value: TameAnimalTrigger): MojangTameAnimalTrigger.TriggerInstance {
            return MojangTameAnimalTrigger.TriggerInstance(
                EntityPredicate.EntityPredicateCompositeAdapter.toNMS(value.player),
                EntityPredicate.EntityPredicateCompositeAdapter.toNMS(value.entity)
            )
        }
        
    }
    
    class Builder : Trigger.Builder<TameAnimalTrigger>() {
        
        private var entity: EntityPredicate? = null
        
        fun entity(init: EntityPredicate.Builder.() -> Unit) {
            entity = EntityPredicate.Builder().apply(init).build()
        }
        
        override fun build(): TameAnimalTrigger {
            return TameAnimalTrigger(player, entity)
        }
        
    }
    
}