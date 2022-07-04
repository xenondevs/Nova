package xyz.xenondevs.nmsutils.advancement.trigger

import net.minecraft.advancements.critereon.DistanceTrigger
import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.nmsutils.adapter.Adapter
import xyz.xenondevs.nmsutils.advancement.predicate.DistancePredicate
import xyz.xenondevs.nmsutils.advancement.predicate.EntityPredicate
import xyz.xenondevs.nmsutils.advancement.predicate.LocationPredicate

class RideEntityInLavaTrigger(
    val player: EntityPredicate?,
    val startLocation: LocationPredicate?,
    val distance: DistancePredicate?
) : Trigger {
    
    companion object : Adapter<RideEntityInLavaTrigger, DistanceTrigger.TriggerInstance> {
        
        private val ID = ResourceLocation("ride_entity_in_lava")
        
        override fun toNMS(value: RideEntityInLavaTrigger): DistanceTrigger.TriggerInstance {
            return DistanceTrigger.TriggerInstance(
                ID,
                EntityPredicate.EntityPredicateCompositeAdapter.toNMS(value.player),
                LocationPredicate.toNMS(value.startLocation),
                DistancePredicate.toNMS(value.distance)
            )
        }
        
    }
    
    class Builder : Trigger.Builder<RideEntityInLavaTrigger>() {
        
        private var startLocation: LocationPredicate? = null
        private var distance: DistancePredicate? = null
        
        fun startLocation(init: LocationPredicate.Builder.() -> Unit) {
            startLocation = LocationPredicate.Builder().apply(init).build()
        }
        
        fun distance(init: DistancePredicate.Builder.() -> Unit) {
            distance = DistancePredicate.Builder().apply(init).build()
        }
        
        override fun build(): RideEntityInLavaTrigger {
            return RideEntityInLavaTrigger(player, startLocation, distance)
        }
        
    }
    
}