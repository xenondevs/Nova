package xyz.xenondevs.nmsutils.advancement.trigger

import net.minecraft.advancements.CriteriaTriggers
import net.minecraft.advancements.critereon.EntityPredicate
import net.minecraft.advancements.critereon.ItemPredicate
import net.minecraft.advancements.critereon.PickedUpItemTrigger
import xyz.xenondevs.nmsutils.advancement.predicate.EntityPredicateBuilder
import xyz.xenondevs.nmsutils.advancement.predicate.ItemPredicateBuilder
import xyz.xenondevs.nmsutils.internal.util.asContextAwarePredicate

class ThrownItemPickedUpByEntityTriggerBuilder : TriggerBuilder<PickedUpItemTrigger.TriggerInstance>() {
    
    private var item = ItemPredicate.ANY
    private var entity = EntityPredicate.ANY
    
    fun item(init: ItemPredicateBuilder.() -> Unit) {
        item = ItemPredicateBuilder().apply(init).build()
    }
    
    fun entity(init: EntityPredicateBuilder.() -> Unit) {
        entity = EntityPredicateBuilder().apply(init).build()
    }
    
    override fun build() = PickedUpItemTrigger.TriggerInstance(
        CriteriaTriggers.THROWN_ITEM_PICKED_UP_BY_ENTITY.id,
        player, item, entity.asContextAwarePredicate()
    )
    
}