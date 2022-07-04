package xyz.xenondevs.nmsutils.advancement.trigger

import xyz.xenondevs.nmsutils.adapter.Adapter
import xyz.xenondevs.nmsutils.advancement.predicate.EntityPredicate
import xyz.xenondevs.nmsutils.advancement.predicate.ItemPredicate
import net.minecraft.advancements.critereon.ConsumeItemTrigger as MojangConsumemItemTrigger

class ConsumeItemTrigger(
    val player: EntityPredicate?,
    val item: ItemPredicate?
) : Trigger {
    
    companion object : Adapter<ConsumeItemTrigger, MojangConsumemItemTrigger.TriggerInstance> {
        
        override fun toNMS(value: ConsumeItemTrigger): MojangConsumemItemTrigger.TriggerInstance {
            return MojangConsumemItemTrigger.TriggerInstance(
                EntityPredicate.EntityPredicateCompositeAdapter.toNMS(value.player),
                ItemPredicate.toNMS(value.item)
            )
        }
        
    }
    
    class Builder : Trigger.Builder<ConsumeItemTrigger>() {
        
        private var item: ItemPredicate? = null
        
        fun item(init: ItemPredicate.Builder.() -> Unit) {
            item = ItemPredicate.Builder().apply(init).build()
        }
        
        override fun build(): ConsumeItemTrigger {
            return ConsumeItemTrigger(player, item)
        }
        
    }
    
}