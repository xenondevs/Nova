package xyz.xenondevs.nmsutils.advancement.trigger

import net.minecraft.advancements.critereon.PlayerTrigger
import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.nmsutils.adapter.Adapter
import xyz.xenondevs.nmsutils.advancement.predicate.EntityPredicate

class HeroOfTheVillageTrigger(
    val player: EntityPredicate?
) : Trigger {
    
    companion object : Adapter<HeroOfTheVillageTrigger, PlayerTrigger.TriggerInstance> {
        
        private val ID = ResourceLocation("hero_of_the_village")
        
        override fun toNMS(value: HeroOfTheVillageTrigger): PlayerTrigger.TriggerInstance {
            return PlayerTrigger.TriggerInstance(
                ID,
                EntityPredicate.EntityPredicateCompositeAdapter.toNMS(value.player)
            )
        }
        
    }
    
    class Builder : Trigger.PlayerBuilder<HeroOfTheVillageTrigger>(::HeroOfTheVillageTrigger)
    
}