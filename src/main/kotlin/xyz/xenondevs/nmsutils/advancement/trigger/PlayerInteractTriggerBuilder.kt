package xyz.xenondevs.nmsutils.advancement.trigger

import net.minecraft.advancements.critereon.EntityPredicate
import net.minecraft.advancements.critereon.ItemPredicate
import net.minecraft.advancements.critereon.PlayerInteractTrigger
import xyz.xenondevs.nmsutils.advancement.predicate.EntityPredicateBuilder
import xyz.xenondevs.nmsutils.advancement.predicate.ItemPredicateBuilder
import xyz.xenondevs.nmsutils.internal.util.asContextAwarePredicate

class PlayerInteractTriggerBuilder : TriggerBuilder<PlayerInteractTrigger.TriggerInstance>() {
    
    private var item = ItemPredicate.ANY
    private var entity = EntityPredicate.ANY
    
    fun item(init: ItemPredicateBuilder.() -> Unit) {
        item = ItemPredicateBuilder().apply(init).build()
    }
    
    fun entity(init: EntityPredicateBuilder.() -> Unit) {
        entity = EntityPredicateBuilder().apply(init).build()
    }
    
    override fun build() = PlayerInteractTrigger.TriggerInstance(
        player,
        item,
        entity.asContextAwarePredicate()
    )
    
}