package xyz.xenondevs.nmsutils.advancement.trigger

import net.minecraft.advancements.critereon.PickedUpItemTrigger
import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.nmsutils.adapter.Adapter
import xyz.xenondevs.nmsutils.advancement.predicate.EntityPredicate
import xyz.xenondevs.nmsutils.advancement.predicate.ItemPredicate

class ThrownItemPickedUpByPlayerTrigger(
    val player: EntityPredicate?,
    val entity: EntityPredicate?,
    val item: ItemPredicate?
) : Trigger {
    
    companion object : Adapter<ThrownItemPickedUpByPlayerTrigger, PickedUpItemTrigger.TriggerInstance> {
        
        private val ID = ResourceLocation("thrown_item_picked_up_by_player")
        
        override fun toNMS(value: ThrownItemPickedUpByPlayerTrigger): PickedUpItemTrigger.TriggerInstance {
            return PickedUpItemTrigger.TriggerInstance(
                ID,
                EntityPredicate.EntityPredicateCompositeAdapter.toNMS(value.player),
                ItemPredicate.toNMS(value.item),
                EntityPredicate.EntityPredicateCompositeAdapter.toNMS(value.entity),
            )
        }
        
    }
    
    class Builder : Trigger.Builder<ThrownItemPickedUpByPlayerTrigger>() {
        
        private var entity: EntityPredicate? = null
        private var item: ItemPredicate? = null
        
        fun entity(init: EntityPredicate.Builder.() -> Unit) {
            entity = EntityPredicate.Builder().apply(init).build()
        }
        
        fun item(init: ItemPredicate.Builder.() -> Unit) {
            item = ItemPredicate.Builder().apply(init).build()
        }
        
        override fun build(): ThrownItemPickedUpByPlayerTrigger {
            return ThrownItemPickedUpByPlayerTrigger(player, entity, item)
        }
        
    }
    
}