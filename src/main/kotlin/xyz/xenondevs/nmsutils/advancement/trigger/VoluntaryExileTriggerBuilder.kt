package xyz.xenondevs.nmsutils.advancement.trigger

import net.minecraft.advancements.CriteriaTriggers
import net.minecraft.advancements.critereon.PlayerTrigger

class VoluntaryExileTriggerBuilder : TriggerBuilder<PlayerTrigger.TriggerInstance>() {
    
    override fun build() = PlayerTrigger.TriggerInstance(CriteriaTriggers.BAD_OMEN.id, player)
    
}