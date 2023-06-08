package xyz.xenondevs.nmsutils.advancement.trigger

import net.minecraft.advancements.critereon.EnchantedItemTrigger
import net.minecraft.advancements.critereon.ItemPredicate
import net.minecraft.advancements.critereon.MinMaxBounds
import xyz.xenondevs.nmsutils.advancement.predicate.ItemPredicateBuilder

class EnchantedItemTriggerBuilder : TriggerBuilder<EnchantedItemTrigger.TriggerInstance>() {
    
    private var item = ItemPredicate.ANY
    private var levels = MinMaxBounds.Ints.ANY
    
    fun item(init: ItemPredicateBuilder.() -> Unit) {
        item = ItemPredicateBuilder().apply(init).build()
    }
    
    fun levels(levels: MinMaxBounds.Ints) {
        this.levels = levels
    }
    
    fun levels(levels: IntRange) {
        this.levels = MinMaxBounds.Ints.between(levels.first, levels.last)
    }
    
    fun levels(levels: Int) {
        this.levels = MinMaxBounds.Ints.exactly(levels)
    }
    
    override fun build(): EnchantedItemTrigger.TriggerInstance {
        return EnchantedItemTrigger.TriggerInstance(player, item, levels)
    }
    
}