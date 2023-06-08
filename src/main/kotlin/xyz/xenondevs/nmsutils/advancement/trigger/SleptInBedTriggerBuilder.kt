package xyz.xenondevs.nmsutils.advancement.trigger

import net.minecraft.advancements.CriteriaTriggers
import net.minecraft.advancements.critereon.PlayerTrigger

class SleptInBedTriggerBuilder : TriggerBuilder<PlayerTrigger.TriggerInstance>() {
    
    override fun build() = PlayerTrigger.TriggerInstance(
        CriteriaTriggers.SLEPT_IN_BED.id,
        player
    )
    
}