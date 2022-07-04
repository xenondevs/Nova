package xyz.xenondevs.nmsutils.advancement.trigger

import xyz.xenondevs.nmsutils.adapter.Adapter
import xyz.xenondevs.nmsutils.advancement.predicate.DamagePredicate
import xyz.xenondevs.nmsutils.advancement.predicate.EntityPredicate
import net.minecraft.advancements.critereon.PlayerHurtEntityTrigger as MojangPlayerHurtEntityTrigger

class PlayerHurtEntityTrigger(
    val player: EntityPredicate?,
    val damage: DamagePredicate?,
    val entity: EntityPredicate?
) : Trigger {
    
    companion object : Adapter<PlayerHurtEntityTrigger, MojangPlayerHurtEntityTrigger.TriggerInstance> {
        
        override fun toNMS(value: PlayerHurtEntityTrigger): MojangPlayerHurtEntityTrigger.TriggerInstance {
            return MojangPlayerHurtEntityTrigger.TriggerInstance(
                EntityPredicate.EntityPredicateCompositeAdapter.toNMS(value.player),
                DamagePredicate.toNMS(value.damage),
                EntityPredicate.EntityPredicateCompositeAdapter.toNMS(value.entity)
            )
        }
        
    }
    
    class Builder : Trigger.Builder<PlayerHurtEntityTrigger>() {
        
        private var damage: DamagePredicate? = null
        private var entity: EntityPredicate? = null
        
        fun damage(init: DamagePredicate.Builder.() -> Unit) {
            damage = DamagePredicate.Builder().apply(init).build()
        }
        
        fun entity(init: EntityPredicate.Builder.() -> Unit) {
            entity = EntityPredicate.Builder().apply(init).build()
        }
        
        override fun build(): PlayerHurtEntityTrigger {
            return PlayerHurtEntityTrigger(player, damage, entity)
        }
        
    }
    
}