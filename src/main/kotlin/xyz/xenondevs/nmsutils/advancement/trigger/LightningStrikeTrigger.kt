package xyz.xenondevs.nmsutils.advancement.trigger

import xyz.xenondevs.nmsutils.adapter.Adapter
import xyz.xenondevs.nmsutils.advancement.predicate.EntityPredicate
import net.minecraft.advancements.critereon.LightningStrikeTrigger as MojangLightningStrikeTrigger

class LightningStrikeTrigger(
    val player: EntityPredicate?,
    val lightning: EntityPredicate?,
    val bystander: EntityPredicate?
) : Trigger {
    
    companion object : Adapter<LightningStrikeTrigger, MojangLightningStrikeTrigger.TriggerInstance> {
        
        override fun toNMS(value: LightningStrikeTrigger): MojangLightningStrikeTrigger.TriggerInstance {
            return MojangLightningStrikeTrigger.TriggerInstance(
                EntityPredicate.EntityPredicateCompositeAdapter.toNMS(value.player),
                EntityPredicate.EntityPredicateCompositeAdapter.toNMS(value.lightning),
                EntityPredicate.EntityPredicateCompositeAdapter.toNMS(value.bystander)
            )
        }
        
    }
    
    class Builder : Trigger.Builder<LightningStrikeTrigger>() {
        
        private var lightning: EntityPredicate? = null
        private var bystander: EntityPredicate? = null
        
        fun lightning(init: EntityPredicate.Builder.() -> Unit) {
            lightning = EntityPredicate.Builder().apply(init).build()
        }
        
        fun bystander(init: EntityPredicate.Builder.() -> Unit) {
            bystander = EntityPredicate.Builder().apply(init).build()
        }
        
        override fun build(): LightningStrikeTrigger {
            return LightningStrikeTrigger(player, lightning, bystander)
        }
        
    }
    
}