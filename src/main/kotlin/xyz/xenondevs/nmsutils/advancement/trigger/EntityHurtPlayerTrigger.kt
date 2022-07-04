package xyz.xenondevs.nmsutils.advancement.trigger

import xyz.xenondevs.nmsutils.adapter.Adapter
import xyz.xenondevs.nmsutils.advancement.predicate.DamagePredicate
import xyz.xenondevs.nmsutils.advancement.predicate.EntityPredicate
import net.minecraft.advancements.critereon.EntityHurtPlayerTrigger as MojangEntityHurtPlayerTrigger

class EntityHurtPlayerTrigger(
    val player: EntityPredicate?,
    val damage: DamagePredicate?
) : Trigger {
    
    companion object : Adapter<EntityHurtPlayerTrigger, MojangEntityHurtPlayerTrigger.TriggerInstance> {
        
        override fun toNMS(value: EntityHurtPlayerTrigger): MojangEntityHurtPlayerTrigger.TriggerInstance {
            return MojangEntityHurtPlayerTrigger.TriggerInstance(
                EntityPredicate.EntityPredicateCompositeAdapter.toNMS(value.player),
                DamagePredicate.toNMS(value.damage)
            )
        }
        
    }
    
    class Builder : Trigger.Builder<EntityHurtPlayerTrigger>() {
        
        private var damage: DamagePredicate? = null
        
        fun damage(init: DamagePredicate.Builder.() -> Unit) {
            damage = DamagePredicate.Builder().apply(init).build()
        }
        
        override fun build(): EntityHurtPlayerTrigger {
            return EntityHurtPlayerTrigger(player, damage)
        }
        
    }
    
}