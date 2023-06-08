package xyz.xenondevs.nmsutils.advancement.trigger

import net.minecraft.advancements.critereon.ConstructBeaconTrigger
import net.minecraft.advancements.critereon.MinMaxBounds

class ConstructBeaconTriggerBuilder : TriggerBuilder<ConstructBeaconTrigger.TriggerInstance>() {
    
    private var level = MinMaxBounds.Ints.ANY
    
    fun level(level: MinMaxBounds.Ints) {
        this.level = level
    }
    
    fun level(level: IntRange) {
        this.level = MinMaxBounds.Ints.between(level.first, level.last)
    }
    
    fun level(level: Int) {
        this.level = MinMaxBounds.Ints.exactly(level)
    }
    
    override fun build(): ConstructBeaconTrigger.TriggerInstance {
        return ConstructBeaconTrigger.TriggerInstance(player, level)
    }
    
}