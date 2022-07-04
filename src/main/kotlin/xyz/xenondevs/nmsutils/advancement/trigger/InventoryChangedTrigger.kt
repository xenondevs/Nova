package xyz.xenondevs.nmsutils.advancement.trigger

import net.minecraft.advancements.critereon.InventoryChangeTrigger
import xyz.xenondevs.nmsutils.adapter.Adapter
import xyz.xenondevs.nmsutils.adapter.impl.IntBoundsAdapter
import xyz.xenondevs.nmsutils.advancement.predicate.EntityPredicate
import xyz.xenondevs.nmsutils.advancement.predicate.ItemPredicate

class InventoryChangedTrigger(
    val player: EntityPredicate?,
    val slotsOccupied: IntRange?,
    val slotsFull: IntRange?,
    val slotsEmpty: IntRange?,
    val items: List<ItemPredicate>?
) : Trigger {
    
    companion object : Adapter<InventoryChangedTrigger, InventoryChangeTrigger.TriggerInstance> {
        
        override fun toNMS(value: InventoryChangedTrigger): InventoryChangeTrigger.TriggerInstance {
            return InventoryChangeTrigger.TriggerInstance(
                EntityPredicate.EntityPredicateCompositeAdapter.toNMS(value.player),
                IntBoundsAdapter.toNMS(value.slotsOccupied),
                IntBoundsAdapter.toNMS(value.slotsFull),
                IntBoundsAdapter.toNMS(value.slotsEmpty),
                value.items?.map(ItemPredicate::toNMS)?.toTypedArray() ?: emptyArray()
            )
        }
        
    }
    
    class Builder : Trigger.Builder<InventoryChangedTrigger>() {
        
        private var slotsOccupied: IntRange? = null
        private var slotsFull: IntRange? = null
        private var slotsEmpty: IntRange? = null
        private val items = ArrayList<ItemPredicate>()
        
        fun item(init: ItemPredicate.Builder.() -> Unit) {
            items += ItemPredicate.Builder().apply(init).build()
        }
        
        fun slotsOccupied(value: IntRange) {
            slotsOccupied = value
        }
        
        fun slotsFull(value: IntRange) {
            slotsFull = value
        }
        
        fun slotsEmpty(value: IntRange) {
            slotsEmpty = value
        }
        
        override fun build(): InventoryChangedTrigger {
            return InventoryChangedTrigger(
                player,
                slotsOccupied,
                slotsFull,
                slotsEmpty,
                items.takeUnless(List<*>::isEmpty)
            )
        }
        
    }
    
}