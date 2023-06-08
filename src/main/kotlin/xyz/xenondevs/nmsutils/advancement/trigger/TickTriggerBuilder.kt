package xyz.xenondevs.nmsutils.advancement.trigger

import net.minecraft.advancements.CriteriaTriggers
import net.minecraft.advancements.critereon.PlayerTrigger

class TickTriggerBuilder : TriggerBuilder<PlayerTrigger.TriggerInstance>() {
    
    override fun build() = PlayerTrigger.TriggerInstance(
        CriteriaTriggers.TICK.id,
        player
    )
    
}