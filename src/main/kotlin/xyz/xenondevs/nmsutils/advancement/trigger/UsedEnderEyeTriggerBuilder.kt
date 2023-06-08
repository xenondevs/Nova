package xyz.xenondevs.nmsutils.advancement.trigger

import net.minecraft.advancements.critereon.MinMaxBounds
import net.minecraft.advancements.critereon.UsedEnderEyeTrigger

class UsedEnderEyeTriggerBuilder : TriggerBuilder<UsedEnderEyeTrigger.TriggerInstance>() {
    
    private var distance = MinMaxBounds.Doubles.ANY
    
    fun distance(distance: MinMaxBounds.Doubles) {
        this.distance = distance
    }
    
    fun distance(distance: ClosedRange<Double>) {
        this.distance = MinMaxBounds.Doubles.between(distance.start, distance.endInclusive)
    }
    
    fun distance(distance: Double) {
        this.distance = MinMaxBounds.Doubles.exactly(distance)
    }
    
    override fun build() = UsedEnderEyeTrigger.TriggerInstance(player, distance)
    
    
}