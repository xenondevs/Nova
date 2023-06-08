package xyz.xenondevs.nmsutils.advancement.trigger

import net.minecraft.advancements.CriteriaTriggers
import net.minecraft.advancements.critereon.ContextAwarePredicate
import net.minecraft.advancements.critereon.DamageSourcePredicate
import net.minecraft.advancements.critereon.KilledTrigger
import xyz.xenondevs.nmsutils.advancement.predicate.DamageSourcePredicateBuilder
import xyz.xenondevs.nmsutils.advancement.predicate.EntityPredicateBuilder
import xyz.xenondevs.nmsutils.internal.util.asContextAwarePredicate

class EntityKilledPlayerTriggerBuilder : TriggerBuilder<KilledTrigger.TriggerInstance>() {
    
    private var entity = ContextAwarePredicate.ANY
    private var damage = DamageSourcePredicate.ANY
    
    fun entity(init: EntityPredicateBuilder.() -> Unit) {
        entity = EntityPredicateBuilder().apply(init).build().asContextAwarePredicate()
    }
    
    fun damage(init: DamageSourcePredicateBuilder.() -> Unit) {
        damage = DamageSourcePredicateBuilder().apply(init).build()
    }
    
    override fun build(): KilledTrigger.TriggerInstance {
        return KilledTrigger.TriggerInstance(
            CriteriaTriggers.ENTITY_KILLED_PLAYER.id,
            player,
            entity,
            damage
        )
    }
    
}