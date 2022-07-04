package xyz.xenondevs.nmsutils.advancement.trigger

import net.minecraft.advancements.critereon.PickedUpItemTrigger
import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.nmsutils.adapter.Adapter
import xyz.xenondevs.nmsutils.advancement.predicate.EntityPredicate
import xyz.xenondevs.nmsutils.advancement.predicate.ItemPredicate

class ThrownItemPickedUpByEntityTrigger(
    val player: EntityPredicate?,
    val item: ItemPredicate?,
    val entity: EntityPredicate?
) : Trigger {
    
    companion object : Adapter<ThrownItemPickedUpByEntityTrigger, PickedUpItemTrigger.TriggerInstance> {
        
        private val ID = ResourceLocation("thrown_item_picked_up_by_entity")
        
        override fun toNMS(value: ThrownItemPickedUpByEntityTrigger): PickedUpItemTrigger.TriggerInstance {
            return PickedUpItemTrigger.TriggerInstance(
                ID,
                EntityPredicate.EntityPredicateCompositeAdapter.toNMS(value.player),
                ItemPredicate.toNMS(value.item),
                EntityPredicate.EntityPredicateCompositeAdapter.toNMS(value.entity)
            )
        }
        
    }
    
    class Builder : Trigger.Builder<ThrownItemPickedUpByEntityTrigger>() {
        
        private var item: ItemPredicate? = null
        private var entity: EntityPredicate? = null
        
        fun item(init: ItemPredicate.Builder.() -> Unit) {
            item = ItemPredicate.Builder().apply(init).build()
        }
        
        fun entity(init: EntityPredicate.Builder.() -> Unit) {
            entity = EntityPredicate.Builder().apply(init).build()
        }
        
        override fun build(): ThrownItemPickedUpByEntityTrigger {
            return ThrownItemPickedUpByEntityTrigger(player, item, entity)
        }
        
    }
    
}