package xyz.xenondevs.nmsutils.advancement.trigger

import net.minecraft.advancements.critereon.PlayerTrigger
import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.nmsutils.adapter.Adapter
import xyz.xenondevs.nmsutils.advancement.predicate.EntityPredicate

class AvoidVibrationTrigger(
    val player: EntityPredicate?
) : Trigger {
    
    companion object : Adapter<AvoidVibrationTrigger, PlayerTrigger.TriggerInstance> {
        
        private val ID = ResourceLocation("avoid_vibration")
        
        override fun toNMS(value: AvoidVibrationTrigger): PlayerTrigger.TriggerInstance {
            return PlayerTrigger.TriggerInstance(
                ID,
                EntityPredicate.EntityPredicateCompositeAdapter.toNMS(value.player)
            )
        }
        
    }
    
    class Builder : Trigger.PlayerBuilder<AvoidVibrationTrigger>(::AvoidVibrationTrigger)
    
}