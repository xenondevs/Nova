package xyz.xenondevs.nmsutils.advancement.trigger

import net.minecraft.advancements.critereon.ContextAwarePredicate
import net.minecraft.advancements.critereon.DamagePredicate
import net.minecraft.advancements.critereon.PlayerHurtEntityTrigger
import xyz.xenondevs.nmsutils.advancement.predicate.DamagePredicateBuilder
import xyz.xenondevs.nmsutils.advancement.predicate.EntityPredicateBuilder
import xyz.xenondevs.nmsutils.internal.util.asContextAwarePredicate

class PlayerHurtEntityTriggerBuilder : TriggerBuilder<PlayerHurtEntityTrigger.TriggerInstance>() {
    
    private var damage = DamagePredicate.ANY
    private var entity = ContextAwarePredicate.ANY
    
    fun damage(init: DamagePredicateBuilder.() -> Unit) {
        damage = DamagePredicateBuilder().apply(init).build()
    }
    
    fun entity(init: EntityPredicateBuilder.() -> Unit) {
        entity = EntityPredicateBuilder().apply(init).build().asContextAwarePredicate()
    }
    
    override fun build(): PlayerHurtEntityTrigger.TriggerInstance {
        return PlayerHurtEntityTrigger.TriggerInstance(player, damage, entity)
    }
    
}