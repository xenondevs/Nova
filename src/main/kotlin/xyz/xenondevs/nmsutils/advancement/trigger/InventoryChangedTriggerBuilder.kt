package xyz.xenondevs.nmsutils.advancement.trigger

import net.minecraft.advancements.critereon.InventoryChangeTrigger
import net.minecraft.advancements.critereon.ItemPredicate
import net.minecraft.advancements.critereon.MinMaxBounds
import xyz.xenondevs.nmsutils.advancement.predicate.ItemPredicateBuilder

class InventoryChangedTriggerBuilder : TriggerBuilder<InventoryChangeTrigger.TriggerInstance>() {
    
    private var slotsOccupied = MinMaxBounds.Ints.ANY
    private var slotsFull = MinMaxBounds.Ints.ANY
    private var slotsEmpty = MinMaxBounds.Ints.ANY
    private val items = ArrayList<ItemPredicate>()
    
    fun item(init: ItemPredicateBuilder.() -> Unit) {
        items += ItemPredicateBuilder().apply(init).build()
    }
    
    fun slotsOccupied(value: MinMaxBounds.Ints) {
        slotsOccupied = value
    }
    
    fun slotsOccupied(value: IntRange) {
        slotsOccupied = MinMaxBounds.Ints.between(value.first, value.last)
    }
    
    fun slotsOccupied(value: Int) {
        slotsOccupied = MinMaxBounds.Ints.exactly(value)
    }
    
    fun slotsFull(value: MinMaxBounds.Ints) {
        slotsFull = value
    }
    
    fun slotsFull(value: IntRange) {
        slotsFull = MinMaxBounds.Ints.between(value.first, value.last)
    }
    
    fun slotsFull(value: Int) {
        slotsFull = MinMaxBounds.Ints.exactly(value)
    }
    
    fun slotsEmpty(value: MinMaxBounds.Ints) {
        slotsEmpty = value
    }
    
    fun slotsEmpty(value: IntRange) {
        slotsEmpty = MinMaxBounds.Ints.between(value.first, value.last)
    }
    
    fun slotsEmpty(value: Int) {
        slotsEmpty = MinMaxBounds.Ints.exactly(value)
    }
    
    override fun build(): InventoryChangeTrigger.TriggerInstance {
        return InventoryChangeTrigger.TriggerInstance(
            player,
            slotsOccupied,
            slotsFull,
            slotsEmpty,
            items.toTypedArray()
        )
    }
    
}