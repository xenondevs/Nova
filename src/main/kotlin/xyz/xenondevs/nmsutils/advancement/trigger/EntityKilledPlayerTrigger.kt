package xyz.xenondevs.nmsutils.advancement.trigger

import net.minecraft.advancements.critereon.KilledTrigger
import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.nmsutils.adapter.Adapter
import xyz.xenondevs.nmsutils.advancement.predicate.DamageSourcePredicate
import xyz.xenondevs.nmsutils.advancement.predicate.EntityPredicate

class EntityKilledPlayerTrigger(
    val player: EntityPredicate?,
    val entity: EntityPredicate?,
    val source: DamageSourcePredicate?
) : Trigger {
    
    companion object : Adapter<EntityKilledPlayerTrigger, KilledTrigger.TriggerInstance> {
        
        private val ID = ResourceLocation("entity_killed_player")
        
        override fun toNMS(value: EntityKilledPlayerTrigger): KilledTrigger.TriggerInstance {
            return KilledTrigger.TriggerInstance(
                ID,
                EntityPredicate.EntityPredicateCompositeAdapter.toNMS(value.player),
                EntityPredicate.EntityPredicateCompositeAdapter.toNMS(value.entity),
                DamageSourcePredicate.toNMS(value.source)
            )
        }
        
    }
    
    class Builder : Trigger.Builder<EntityKilledPlayerTrigger>() {
        
        private var entity: EntityPredicate? = null
        private var damage: DamageSourcePredicate? = null
        
        fun entity(init: EntityPredicate.Builder.() -> Unit) {
            entity = EntityPredicate.Builder().apply(init).build()
        }
        
        fun damage(init: DamageSourcePredicate.Builder.() -> Unit) {
            damage = DamageSourcePredicate.Builder().apply(init).build()
        }
        
        override fun build(): EntityKilledPlayerTrigger {
            return EntityKilledPlayerTrigger(player, entity, damage)
        }
        
    }
}