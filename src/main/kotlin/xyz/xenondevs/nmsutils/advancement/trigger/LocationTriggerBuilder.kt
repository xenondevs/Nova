package xyz.xenondevs.nmsutils.advancement.trigger

import net.minecraft.advancements.CriteriaTriggers
import net.minecraft.advancements.critereon.PlayerTrigger

class LocationTriggerBuilder : TriggerBuilder<PlayerTrigger.TriggerInstance>() {
    
    override fun build() = PlayerTrigger.TriggerInstance(
        CriteriaTriggers.LOCATION.id,
        player
    )
    
}