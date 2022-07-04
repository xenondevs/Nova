package xyz.xenondevs.nmsutils.advancement.trigger

import net.minecraft.advancements.critereon.PlayerTrigger
import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.nmsutils.adapter.Adapter
import xyz.xenondevs.nmsutils.advancement.predicate.EntityPredicate

class LocationTrigger(
    val player: EntityPredicate?
) : Trigger {
    
    companion object : Adapter<LocationTrigger, PlayerTrigger.TriggerInstance> {
        
        val ID = ResourceLocation("location")
        
        override fun toNMS(value: LocationTrigger): PlayerTrigger.TriggerInstance {
            return PlayerTrigger.TriggerInstance(
                ID,
                EntityPredicate.EntityPredicateCompositeAdapter.toNMS(value.player)
            )
        }
        
    }
    
    class Builder : Trigger.PlayerBuilder<LocationTrigger>(::LocationTrigger)
    
}