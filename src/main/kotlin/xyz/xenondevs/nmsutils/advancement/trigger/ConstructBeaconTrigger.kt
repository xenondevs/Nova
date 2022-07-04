package xyz.xenondevs.nmsutils.advancement.trigger

import xyz.xenondevs.nmsutils.adapter.Adapter
import xyz.xenondevs.nmsutils.adapter.impl.IntBoundsAdapter
import xyz.xenondevs.nmsutils.advancement.predicate.EntityPredicate
import net.minecraft.advancements.critereon.ConstructBeaconTrigger as MojangConstructBeaconTrigger

class ConstructBeaconTrigger(
    val player: EntityPredicate?,
    val level: IntRange?
) : Trigger {
    
    companion object : Adapter<ConstructBeaconTrigger, MojangConstructBeaconTrigger.TriggerInstance> {
        
        override fun toNMS(value: ConstructBeaconTrigger): MojangConstructBeaconTrigger.TriggerInstance {
            return MojangConstructBeaconTrigger.TriggerInstance(
                EntityPredicate.EntityPredicateCompositeAdapter.toNMS(value.player),
                IntBoundsAdapter.toNMS(value.level)
            )
        }
        
    }
    
    class Builder : Trigger.Builder<ConstructBeaconTrigger>() {
        
        private var level: IntRange? = null
        
        fun level(level: IntRange) {
            this.level = level
        }
        
        override fun build(): ConstructBeaconTrigger {
            return ConstructBeaconTrigger(player, level)
        }
        
    }
    
}