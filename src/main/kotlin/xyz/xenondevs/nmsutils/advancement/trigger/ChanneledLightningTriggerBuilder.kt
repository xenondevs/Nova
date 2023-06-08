package xyz.xenondevs.nmsutils.advancement.trigger

import net.minecraft.advancements.critereon.ChanneledLightningTrigger
import net.minecraft.advancements.critereon.ContextAwarePredicate
import xyz.xenondevs.nmsutils.advancement.predicate.EntityPredicateBuilder
import xyz.xenondevs.nmsutils.internal.util.asContextAwarePredicate

class ChanneledLightningTriggerBuilder : TriggerBuilder<ChanneledLightningTrigger.TriggerInstance>() {
    
    private var victims = ArrayList<ContextAwarePredicate>()
    
    fun victim(init: EntityPredicateBuilder.() -> Unit) {
        victims += EntityPredicateBuilder().apply(init).build().asContextAwarePredicate()
    }
    
    override fun build(): ChanneledLightningTrigger.TriggerInstance {
        return ChanneledLightningTrigger.TriggerInstance(player, victims.toTypedArray())
    }
    
}