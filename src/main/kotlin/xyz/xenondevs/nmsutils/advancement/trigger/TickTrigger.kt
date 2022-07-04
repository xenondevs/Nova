package xyz.xenondevs.nmsutils.advancement.trigger

import net.minecraft.advancements.critereon.PlayerTrigger
import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.nmsutils.adapter.Adapter
import xyz.xenondevs.nmsutils.advancement.predicate.EntityPredicate

class TickTrigger(
    val player: EntityPredicate?
) : Trigger {
    
    companion object : Adapter<TickTrigger, PlayerTrigger.TriggerInstance> {
        
        private val ID = ResourceLocation("tick")
        
        override fun toNMS(value: TickTrigger): PlayerTrigger.TriggerInstance {
            return PlayerTrigger.TriggerInstance(
                ID,
                EntityPredicate.EntityPredicateCompositeAdapter.toNMS(value.player)
            )
        }
        
    }
    
    class Builder : Trigger.PlayerBuilder<TickTrigger>(::TickTrigger)
    
}