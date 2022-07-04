package xyz.xenondevs.nmsutils.advancement.trigger

import net.minecraft.advancements.critereon.TargetBlockTrigger
import xyz.xenondevs.nmsutils.adapter.Adapter
import xyz.xenondevs.nmsutils.adapter.impl.IntBoundsAdapter
import xyz.xenondevs.nmsutils.advancement.predicate.EntityPredicate

class TargetHitTrigger(
    val player: EntityPredicate?,
    val signalStrength: IntRange?,
    val projectile: EntityPredicate?
) : Trigger {
    
    companion object : Adapter<TargetHitTrigger, TargetBlockTrigger.TriggerInstance> {
        
        override fun toNMS(value: TargetHitTrigger): TargetBlockTrigger.TriggerInstance {
            return TargetBlockTrigger.TriggerInstance(
                EntityPredicate.EntityPredicateCompositeAdapter.toNMS(value.player),
                IntBoundsAdapter.toNMS(value.signalStrength),
                EntityPredicate.EntityPredicateCompositeAdapter.toNMS(value.projectile)
            )
        }
        
    }
    
    class Builder : Trigger.Builder<TargetHitTrigger>() {
        
        private var signalStrength: IntRange? = null
        private var projectile: EntityPredicate? = null
        
        fun signalStrength(signalStrength: IntRange) {
            this.signalStrength = signalStrength
        }
        
        fun projectile(init: EntityPredicate.Builder.() -> Unit) {
            this.projectile = EntityPredicate.Builder().apply(init).build()
        }
        
        override fun build(): TargetHitTrigger {
            return TargetHitTrigger(player, signalStrength, projectile)
        }
        
    }
    
}