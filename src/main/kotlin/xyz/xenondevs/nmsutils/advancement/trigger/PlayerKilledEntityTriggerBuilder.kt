package xyz.xenondevs.nmsutils.advancement.trigger

import net.minecraft.advancements.CriteriaTriggers
import net.minecraft.advancements.critereon.DamageSourcePredicate
import net.minecraft.advancements.critereon.EntityPredicate
import net.minecraft.advancements.critereon.KilledTrigger
import xyz.xenondevs.nmsutils.advancement.predicate.DamageSourcePredicateBuilder
import xyz.xenondevs.nmsutils.advancement.predicate.EntityPredicateBuilder
import xyz.xenondevs.nmsutils.internal.util.asContextAwarePredicate

class PlayerKilledEntityTriggerBuilder : TriggerBuilder<KilledTrigger.TriggerInstance>() {
    
    private var source = DamageSourcePredicate.ANY
    private var entity = EntityPredicate.ANY
    
    fun source(init: DamageSourcePredicateBuilder.() -> Unit) {
        source = DamageSourcePredicateBuilder().apply(init).build()
    }
    
    fun entity(init: EntityPredicateBuilder.() -> Unit) {
        entity = EntityPredicateBuilder().apply(init).build()
    }
    
    override fun build() = KilledTrigger.TriggerInstance(
        CriteriaTriggers.PLAYER_KILLED_ENTITY.id,
        player,
        entity.asContextAwarePredicate(),
        source
    )
    
}