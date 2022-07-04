package xyz.xenondevs.nmsutils.advancement.trigger

import xyz.xenondevs.nmsutils.adapter.Adapter
import xyz.xenondevs.nmsutils.advancement.predicate.EntityPredicate
import net.minecraft.advancements.critereon.BredAnimalsTrigger as MojangBredAnimalsTrigger

class BredAnimalsTrigger(
    val player: EntityPredicate?,
    val parent: EntityPredicate?,
    val partner: EntityPredicate?,
    val child: EntityPredicate?
) : Trigger {
    
    companion object : Adapter<BredAnimalsTrigger, MojangBredAnimalsTrigger.TriggerInstance> {
        
        override fun toNMS(value: BredAnimalsTrigger): MojangBredAnimalsTrigger.TriggerInstance {
            return MojangBredAnimalsTrigger.TriggerInstance(
                EntityPredicate.EntityPredicateCompositeAdapter.toNMS(value.player),
                EntityPredicate.EntityPredicateCompositeAdapter.toNMS(value.parent),
                EntityPredicate.EntityPredicateCompositeAdapter.toNMS(value.partner),
                EntityPredicate.EntityPredicateCompositeAdapter.toNMS(value.child),
            )
        }
        
    }
    
    class Builder : Trigger.Builder<BredAnimalsTrigger>() {
        
        private var parent: EntityPredicate? = null
        private var partner: EntityPredicate? = null
        private var child: EntityPredicate? = null
        
        fun parent(init: EntityPredicate.Builder.() -> Unit) {
            parent = EntityPredicate.Builder().apply(init).build()
        }
        
        fun partner(init: EntityPredicate.Builder.() -> Unit) {
            partner = EntityPredicate.Builder().apply(init).build()
        }
        
        fun child(init: EntityPredicate.Builder.() -> Unit) {
            child = EntityPredicate.Builder().apply(init).build()
        }
        
        override fun build(): BredAnimalsTrigger {
            return BredAnimalsTrigger(player, parent, partner, child)
        }
        
    }
    
}