package xyz.xenondevs.nmsutils.advancement.trigger

import net.minecraft.advancements.critereon.BrewedPotionTrigger
import net.minecraft.world.item.alchemy.Potion

class BrewedPotionTriggerBuilder : TriggerBuilder<BrewedPotionTrigger.TriggerInstance>() {
    
    private var potion: Potion? = null
    
    fun potion(potion: Potion) {
        this.potion = potion
    }
    
    override fun build(): BrewedPotionTrigger.TriggerInstance {
        return BrewedPotionTrigger.TriggerInstance(player, potion)
    }
    
}