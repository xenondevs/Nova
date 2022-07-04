package xyz.xenondevs.nmsutils.advancement.trigger

import net.minecraft.advancements.critereon.ItemInteractWithBlockTrigger
import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.nmsutils.adapter.Adapter
import xyz.xenondevs.nmsutils.advancement.predicate.EntityPredicate
import xyz.xenondevs.nmsutils.advancement.predicate.ItemPredicate
import xyz.xenondevs.nmsutils.advancement.predicate.LocationPredicate

class ItemUsedOnBlockTrigger(
    val player: EntityPredicate?,
    val location: LocationPredicate?,
    val item: ItemPredicate?
) : Trigger {
    
    companion object : Adapter<ItemUsedOnBlockTrigger, ItemInteractWithBlockTrigger.TriggerInstance> {
        
        private val ID = ResourceLocation("item_used_on_block")
        
        override fun toNMS(value: ItemUsedOnBlockTrigger): ItemInteractWithBlockTrigger.TriggerInstance {
            return ItemInteractWithBlockTrigger.TriggerInstance(
                ID,
                EntityPredicate.EntityPredicateCompositeAdapter.toNMS(value.player),
                LocationPredicate.toNMS(value.location),
                ItemPredicate.toNMS(value.item)
            )
        }
        
    }
    
    class Builder : Trigger.Builder<ItemUsedOnBlockTrigger>() {
        
        private var location: LocationPredicate? = null
        private var item: ItemPredicate? = null
        
        fun location(init: LocationPredicate.Builder.() -> Unit) {
            location = LocationPredicate.Builder().apply(init).build()
        }
        
        fun item(init: ItemPredicate.Builder.() -> Unit) {
            item = ItemPredicate.Builder().apply(init).build()
        }
        
        override fun build(): ItemUsedOnBlockTrigger {
            return ItemUsedOnBlockTrigger(player, location, item)
        }
        
    }
    
}