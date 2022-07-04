package xyz.xenondevs.nmsutils.advancement.trigger

import net.minecraft.advancements.critereon.PlayerTrigger
import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.nmsutils.adapter.Adapter
import xyz.xenondevs.nmsutils.advancement.predicate.EntityPredicate

class SleptInBedTrigger(
    val player: EntityPredicate?
) : Trigger {
    
    companion object : Adapter<SleptInBedTrigger, PlayerTrigger.TriggerInstance> {
        
        private val ID = ResourceLocation("slept_in_bed")
        
        override fun toNMS(value: SleptInBedTrigger): PlayerTrigger.TriggerInstance {
            return PlayerTrigger.TriggerInstance(
                ID,
                EntityPredicate.EntityPredicateCompositeAdapter.toNMS(value.player)
            )
        }
        
    }
    
    class Builder : Trigger.PlayerBuilder<SleptInBedTrigger>(::SleptInBedTrigger)
    
}