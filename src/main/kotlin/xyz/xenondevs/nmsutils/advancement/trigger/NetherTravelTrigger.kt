package xyz.xenondevs.nmsutils.advancement.trigger

import net.minecraft.advancements.critereon.DistanceTrigger
import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.nmsutils.adapter.Adapter
import xyz.xenondevs.nmsutils.advancement.predicate.DistancePredicate
import xyz.xenondevs.nmsutils.advancement.predicate.EntityPredicate
import xyz.xenondevs.nmsutils.advancement.predicate.LocationPredicate

class NetherTravelTrigger(
    val player: EntityPredicate?,
    val predicate: EntityPredicate?,
    val startLocation: LocationPredicate?,
    val distance: DistancePredicate?
) : Trigger {
    
    companion object : Adapter<NetherTravelTrigger, DistanceTrigger.TriggerInstance> {
        
        private val ID = ResourceLocation("nether_travel")
        
        override fun toNMS(value: NetherTravelTrigger): DistanceTrigger.TriggerInstance {
            return DistanceTrigger.TriggerInstance(
                ID,
                EntityPredicate.EntityPredicateCompositeAdapter.toNMS(value.player),
                LocationPredicate.toNMS(value.startLocation),
                DistancePredicate.toNMS(value.distance)
            )
        }
        
    }
    
    class Builder : Trigger.Builder<NetherTravelTrigger>() {
        
        private var predicate: EntityPredicate? = null
        private var startLocation: LocationPredicate? = null
        private var distance: DistancePredicate? = null
        
        fun predicate(init: EntityPredicate.Builder.() -> Unit) {
            predicate = EntityPredicate.Builder().apply(init).build()
        }
        
        fun startLocation(init: LocationPredicate.Builder.() -> Unit) {
            startLocation = LocationPredicate.Builder().apply(init).build()
        }
        
        fun distance(init: DistancePredicate.Builder.() -> Unit) {
            distance = DistancePredicate.Builder().apply(init).build()
        }
        
        override fun build(): NetherTravelTrigger {
            return NetherTravelTrigger(player, predicate, startLocation, distance)
        }
        
    }
    
}