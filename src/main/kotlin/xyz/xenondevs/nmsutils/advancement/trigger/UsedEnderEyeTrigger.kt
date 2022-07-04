package xyz.xenondevs.nmsutils.advancement.trigger

import xyz.xenondevs.nmsutils.adapter.Adapter
import xyz.xenondevs.nmsutils.adapter.impl.DoubleBoundsAdapter
import xyz.xenondevs.nmsutils.advancement.predicate.EntityPredicate
import net.minecraft.advancements.critereon.UsedEnderEyeTrigger as MojangUsedEnderEyeTrigger

class UsedEnderEyeTrigger(
    val player: EntityPredicate?,
    val distance: ClosedRange<Double>?
) : Trigger {
    
    companion object : Adapter<UsedEnderEyeTrigger, MojangUsedEnderEyeTrigger.TriggerInstance> {
        
        override fun toNMS(value: UsedEnderEyeTrigger): MojangUsedEnderEyeTrigger.TriggerInstance {
            return MojangUsedEnderEyeTrigger.TriggerInstance(
                EntityPredicate.EntityPredicateCompositeAdapter.toNMS(value.player),
                DoubleBoundsAdapter.toNMS(value.distance)
            )
        }
        
    }
    
    class Builder : Trigger.Builder<UsedEnderEyeTrigger>() {
        
        private var distance: ClosedRange<Double>? = null
        
        fun distance(distance: ClosedRange<Double>) {
            this.distance = distance
        }
        
        override fun build(): UsedEnderEyeTrigger {
            return UsedEnderEyeTrigger(player, distance)
        }
        
    }
    
}