package xyz.xenondevs.nmsutils.advancement.trigger

import net.minecraft.advancements.critereon.EntityPredicate
import net.minecraft.advancements.critereon.ItemPredicate
import net.minecraft.advancements.critereon.TradeTrigger
import xyz.xenondevs.nmsutils.advancement.predicate.EntityPredicateBuilder
import xyz.xenondevs.nmsutils.advancement.predicate.ItemPredicateBuilder
import xyz.xenondevs.nmsutils.internal.util.asContextAwarePredicate

class TradeTriggerBuilder : TriggerBuilder<TradeTrigger.TriggerInstance>() {
    
    private var villager = EntityPredicate.ANY
    private var item = ItemPredicate.ANY
    
    fun villager(init: EntityPredicateBuilder.() -> Unit) {
        villager = EntityPredicateBuilder().apply(init).build()
    }
    
    fun item(init: ItemPredicateBuilder.() -> Unit) {
        item = ItemPredicateBuilder().apply(init).build()
    }
    
    override fun build() = TradeTrigger.TriggerInstance(player, villager.asContextAwarePredicate(), item)
    
}