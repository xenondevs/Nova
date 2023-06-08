package xyz.xenondevs.nmsutils.advancement.trigger

import net.minecraft.advancements.critereon.ItemDurabilityTrigger
import net.minecraft.advancements.critereon.ItemPredicate
import net.minecraft.advancements.critereon.MinMaxBounds
import xyz.xenondevs.nmsutils.advancement.predicate.ItemPredicateBuilder

class ItemDurabilityChangedTriggerBuilder : TriggerBuilder<ItemDurabilityTrigger.TriggerInstance>() {
    
    private var delta = MinMaxBounds.Ints.ANY
    private var durability = MinMaxBounds.Ints.ANY
    private var item = ItemPredicate.ANY
    
    fun delta(delta: MinMaxBounds.Ints) {
        this.delta = delta
    }
    
    fun delta(delta: IntRange) {
        this.delta = MinMaxBounds.Ints.between(delta.first, delta.last)
    }
    
    fun delta(delta: Int) {
        this.delta = MinMaxBounds.Ints.exactly(delta)
    }
    
    fun durability(durability: MinMaxBounds.Ints) {
        this.durability = durability
    }
    
    fun durability(durability: IntRange) {
        this.durability = MinMaxBounds.Ints.between(durability.first, durability.last)
    }
    
    fun durability(durability: Int) {
        this.durability = MinMaxBounds.Ints.exactly(durability)
    }
    
    fun item(init: ItemPredicateBuilder.() -> Unit) {
        this.item = ItemPredicateBuilder().apply(init).build()
    }
    
    override fun build(): ItemDurabilityTrigger.TriggerInstance {
        return ItemDurabilityTrigger.TriggerInstance(player, item, durability, delta)
    }
    
}