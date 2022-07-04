package xyz.xenondevs.nmsutils.advancement.trigger

import net.minecraft.advancements.critereon.ChangeDimensionTrigger
import org.bukkit.World
import xyz.xenondevs.nmsutils.adapter.Adapter
import xyz.xenondevs.nmsutils.advancement.predicate.EntityPredicate
import xyz.xenondevs.nmsutils.util.resourceKey

class ChangedDimensionTrigger(
    val player: EntityPredicate?,
    val from: World?,
    val to: World?
) : Trigger {
    
    companion object : Adapter<ChangedDimensionTrigger, ChangeDimensionTrigger.TriggerInstance> {
        
        override fun toNMS(value: ChangedDimensionTrigger): ChangeDimensionTrigger.TriggerInstance {
            return ChangeDimensionTrigger.TriggerInstance(
                EntityPredicate.EntityPredicateCompositeAdapter.toNMS(value.player),
                value.from?.resourceKey,
                value.to?.resourceKey
            )
        }
        
    }
    
    class Builder : Trigger.Builder<ChangedDimensionTrigger>() {
        
        private var from: World? = null
        private var to: World? = null
        
        fun from(world: World) {
            from = world
        }
        
        fun to(world: World) {
            to = world
        }
        
        override fun build(): ChangedDimensionTrigger {
            return ChangedDimensionTrigger(player, from, to)
        }
        
    }
    
}