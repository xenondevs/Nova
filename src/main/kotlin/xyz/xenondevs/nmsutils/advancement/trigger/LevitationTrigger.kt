package xyz.xenondevs.nmsutils.advancement.trigger

import xyz.xenondevs.nmsutils.adapter.Adapter
import xyz.xenondevs.nmsutils.adapter.impl.IntBoundsAdapter
import xyz.xenondevs.nmsutils.advancement.predicate.DistancePredicate
import xyz.xenondevs.nmsutils.advancement.predicate.EntityPredicate
import net.minecraft.advancements.critereon.LevitationTrigger as MojangLevitationTrigger

class LevitationTrigger(
    val player: EntityPredicate?,
    val distance: DistancePredicate?,
    val duration: IntRange?
) : Trigger {
    
    companion object : Adapter<LevitationTrigger, MojangLevitationTrigger.TriggerInstance> {
        
        override fun toNMS(value: LevitationTrigger): MojangLevitationTrigger.TriggerInstance {
            return MojangLevitationTrigger.TriggerInstance(
                EntityPredicate.EntityPredicateCompositeAdapter.toNMS(value.player),
                DistancePredicate.toNMS(value.distance),
                IntBoundsAdapter.toNMS(value.duration)
            )
        }
        
    }
    
    class Builder : Trigger.Builder<LevitationTrigger>() {
        
        private var distance: DistancePredicate? = null
        private var duration: IntRange? = null
        
        fun distance(init: DistancePredicate.Builder.() -> Unit) {
            distance = DistancePredicate.Builder().apply(init).build()
        }
        
        fun duration(duration: IntRange) {
            this.duration = duration
        }
        
        override fun build(): LevitationTrigger {
            return LevitationTrigger(player, distance, duration)
        }
        
    }
    
}