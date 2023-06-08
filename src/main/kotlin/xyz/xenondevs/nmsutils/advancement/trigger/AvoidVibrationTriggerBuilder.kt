package xyz.xenondevs.nmsutils.advancement.trigger

import net.minecraft.advancements.CriteriaTriggers
import net.minecraft.advancements.critereon.PlayerTrigger

class AvoidVibrationTriggerBuilder : TriggerBuilder<PlayerTrigger.TriggerInstance>() {
    
    override fun build(): PlayerTrigger.TriggerInstance =
        PlayerTrigger.TriggerInstance(
            CriteriaTriggers.AVOID_VIBRATION.id,
            player
        )
    
}