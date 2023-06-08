package xyz.xenondevs.nmsutils.advancement.trigger

import net.minecraft.advancements.critereon.DamagePredicate
import net.minecraft.advancements.critereon.EntityHurtPlayerTrigger
import xyz.xenondevs.nmsutils.advancement.predicate.DamagePredicateBuilder

class EntityHurtPlayerTriggerBuilder : TriggerBuilder<EntityHurtPlayerTrigger.TriggerInstance>() {
    
    private var damage = DamagePredicate.ANY
    
    fun damage(init: DamagePredicateBuilder.() -> Unit) {
        damage = DamagePredicateBuilder().apply(init).build()
    }
    
    override fun build(): EntityHurtPlayerTrigger.TriggerInstance {
        return EntityHurtPlayerTrigger.TriggerInstance(player, damage)
    }
    
}