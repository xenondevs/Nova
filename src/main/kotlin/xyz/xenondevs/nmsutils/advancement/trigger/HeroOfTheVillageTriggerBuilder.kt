package xyz.xenondevs.nmsutils.advancement.trigger

import net.minecraft.advancements.CriteriaTriggers
import net.minecraft.advancements.critereon.PlayerTrigger

class HeroOfTheVillageTriggerBuilder : TriggerBuilder<PlayerTrigger.TriggerInstance>() {
    
    override fun build() = PlayerTrigger.TriggerInstance(
        CriteriaTriggers.RAID_WIN.id,
        player
    )
    
}