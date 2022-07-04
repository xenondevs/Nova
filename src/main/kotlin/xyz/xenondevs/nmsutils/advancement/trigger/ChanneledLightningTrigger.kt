package xyz.xenondevs.nmsutils.advancement.trigger

import xyz.xenondevs.nmsutils.adapter.Adapter
import xyz.xenondevs.nmsutils.advancement.predicate.EntityPredicate
import xyz.xenondevs.nmsutils.util.mapToArray
import net.minecraft.advancements.critereon.ChanneledLightningTrigger as MojangChanneledLightningTrigger

class ChanneledLightningTrigger(
    val player: EntityPredicate?,
    val victims: List<EntityPredicate>?
) : Trigger {
    
    companion object : Adapter<ChanneledLightningTrigger, MojangChanneledLightningTrigger.TriggerInstance> {
        
        override fun toNMS(value: ChanneledLightningTrigger): MojangChanneledLightningTrigger.TriggerInstance {
            return MojangChanneledLightningTrigger.TriggerInstance(
                EntityPredicate.EntityPredicateCompositeAdapter.toNMS(value.player),
                value.victims?.mapToArray(EntityPredicate.EntityPredicateCompositeAdapter::toNMS) ?: emptyArray()
            )
        }
        
    }
    
    class Builder : Trigger.Builder<ChanneledLightningTrigger>() {
        
        private var victims = ArrayList<EntityPredicate>()
        
        fun victim(init: EntityPredicate.Builder.() -> Unit) {
            victims += EntityPredicate.Builder().apply(init).build()
        }
        
        override fun build(): ChanneledLightningTrigger {
            return ChanneledLightningTrigger(player, victims.takeUnless(List<*>::isEmpty))
        }
        
    }
    
}