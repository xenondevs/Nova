package xyz.xenondevs.nmsutils.advancement.trigger

import net.minecraft.advancements.critereon.ItemPredicate
import net.minecraft.advancements.critereon.ShotCrossbowTrigger
import xyz.xenondevs.nmsutils.advancement.predicate.ItemPredicateBuilder

class ShotCrossbowTriggerBuilder : TriggerBuilder<ShotCrossbowTrigger.TriggerInstance>() {
    
    private var item = ItemPredicate.ANY
    
    fun item(init: ItemPredicateBuilder.() -> Unit) {
        item = ItemPredicateBuilder().apply(init).build()
    }
    
    override fun build() = ShotCrossbowTrigger.TriggerInstance(
        player,
        item
    )
    
}