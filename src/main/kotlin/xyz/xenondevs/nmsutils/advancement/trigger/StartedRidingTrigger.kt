package xyz.xenondevs.nmsutils.advancement.trigger

import net.minecraft.advancements.critereon.StartRidingTrigger
import xyz.xenondevs.nmsutils.adapter.Adapter
import xyz.xenondevs.nmsutils.advancement.predicate.EntityPredicate

class StartedRidingTrigger(
    val player: EntityPredicate?
) : Trigger {
    
    companion object : Adapter<StartedRidingTrigger, StartRidingTrigger.TriggerInstance> {
        
        override fun toNMS(value: StartedRidingTrigger): StartRidingTrigger.TriggerInstance {
            return StartRidingTrigger.TriggerInstance(EntityPredicate.EntityPredicateCompositeAdapter.toNMS(value.player))
        }
        
    }
    
    class Builder : Trigger.PlayerBuilder<StartedRidingTrigger>(::StartedRidingTrigger)
    
}