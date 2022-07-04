package xyz.xenondevs.nmsutils.advancement.trigger

import net.minecraft.advancements.critereon.PlayerTrigger
import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.nmsutils.adapter.Adapter
import xyz.xenondevs.nmsutils.advancement.predicate.EntityPredicate

class VoluntaryExileTrigger(
    val player: EntityPredicate?
) : Trigger {
    
    companion object : Adapter<VoluntaryExileTrigger, PlayerTrigger.TriggerInstance> {
        
        private val ID = ResourceLocation("voluntary_exile")
        
        override fun toNMS(value: VoluntaryExileTrigger): PlayerTrigger.TriggerInstance {
            return PlayerTrigger.TriggerInstance(
                ID,
                EntityPredicate.EntityPredicateCompositeAdapter.toNMS(value.player)
            )
        }
        
    }
    
    class Builder : Trigger.PlayerBuilder<VoluntaryExileTrigger>(::VoluntaryExileTrigger)
    
}