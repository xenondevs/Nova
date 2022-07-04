package xyz.xenondevs.nmsutils.advancement.trigger

import net.minecraft.advancements.critereon.KilledTrigger
import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.nmsutils.adapter.Adapter
import xyz.xenondevs.nmsutils.advancement.predicate.DamageSourcePredicate
import xyz.xenondevs.nmsutils.advancement.predicate.EntityPredicate

class PlayerKilledEntityTrigger(
    val player: EntityPredicate?,
    val source: DamageSourcePredicate?,
    val entity: EntityPredicate?
) : Trigger {
    
    companion object : Adapter<PlayerKilledEntityTrigger, KilledTrigger.TriggerInstance> {
        
        private val ID = ResourceLocation("player_killed_entity")
        
        override fun toNMS(value: PlayerKilledEntityTrigger): KilledTrigger.TriggerInstance {
            return KilledTrigger.TriggerInstance(
                ID,
                EntityPredicate.EntityPredicateCompositeAdapter.toNMS(value.player),
                EntityPredicate.EntityPredicateCompositeAdapter.toNMS(value.entity),
                DamageSourcePredicate.toNMS(value.source)
            )
        }
        
    }
    
    class Builder : Trigger.Builder<PlayerKilledEntityTrigger>() {
        
        private var source: DamageSourcePredicate? = null
        private var entity: EntityPredicate? = null
        
        fun source(init: DamageSourcePredicate.Builder.() -> Unit) {
            source = DamageSourcePredicate.Builder().apply(init).build()
        }
        
        fun entity(init: EntityPredicate.Builder.() -> Unit) {
            entity = EntityPredicate.Builder().apply(init).build()
        }
        
        override fun build(): PlayerKilledEntityTrigger {
            return PlayerKilledEntityTrigger(player, source, entity)
        }
        
    }
    
}