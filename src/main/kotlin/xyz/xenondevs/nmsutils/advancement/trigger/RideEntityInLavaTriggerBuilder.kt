package xyz.xenondevs.nmsutils.advancement.trigger

import net.minecraft.advancements.CriteriaTriggers
import net.minecraft.advancements.critereon.DistancePredicate
import net.minecraft.advancements.critereon.DistanceTrigger
import net.minecraft.advancements.critereon.LocationPredicate
import xyz.xenondevs.nmsutils.advancement.predicate.DistancePredicateBuilder
import xyz.xenondevs.nmsutils.advancement.predicate.LocationPredicateBuilder

class RideEntityInLavaTriggerBuilder : TriggerBuilder<DistanceTrigger.TriggerInstance>() {
    
    private var startLocation = LocationPredicate.ANY
    private var distance = DistancePredicate.ANY
    
    fun startLocation(init: LocationPredicateBuilder.() -> Unit) {
        startLocation = LocationPredicateBuilder().apply(init).build()
    }
    
    fun distance(init: DistancePredicateBuilder.() -> Unit) {
        distance = DistancePredicateBuilder().apply(init).build()
    }
    
    override fun build() = DistanceTrigger.TriggerInstance(
        CriteriaTriggers.RIDE_ENTITY_IN_LAVA_TRIGGER.id,
        player,
        startLocation,
        distance
    )
    
}