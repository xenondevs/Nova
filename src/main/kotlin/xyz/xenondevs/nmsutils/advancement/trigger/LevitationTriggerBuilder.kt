package xyz.xenondevs.nmsutils.advancement.trigger

import net.minecraft.advancements.critereon.DistancePredicate
import net.minecraft.advancements.critereon.LevitationTrigger
import net.minecraft.advancements.critereon.MinMaxBounds
import xyz.xenondevs.nmsutils.advancement.predicate.DistancePredicateBuilder

class LevitationTriggerBuilder : TriggerBuilder<LevitationTrigger.TriggerInstance>() {
    
    private var distance = DistancePredicate.ANY
    private var duration = MinMaxBounds.Ints.ANY
    
    fun distance(init: DistancePredicateBuilder.() -> Unit) {
        distance = DistancePredicateBuilder().apply(init).build()
    }
    
    fun duration(duration: MinMaxBounds.Ints) {
        this.duration = duration
    }
    
    fun duration(duration: IntRange) {
        this.duration = MinMaxBounds.Ints.between(duration.first, duration.last)
    }
    
    fun duration(duration: Int) {
        this.duration = MinMaxBounds.Ints.exactly(duration)
    }
    
    override fun build(): LevitationTrigger.TriggerInstance {
        return LevitationTrigger.TriggerInstance(player, distance, duration)
    }
    
}