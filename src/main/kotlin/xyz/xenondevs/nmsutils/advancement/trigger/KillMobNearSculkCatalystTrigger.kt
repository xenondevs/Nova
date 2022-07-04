package xyz.xenondevs.nmsutils.advancement.trigger

import net.minecraft.advancements.critereon.KilledTrigger
import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.nmsutils.adapter.Adapter
import xyz.xenondevs.nmsutils.advancement.predicate.DamageSourcePredicate
import xyz.xenondevs.nmsutils.advancement.predicate.EntityPredicate

class KillMobNearSculkCatalystTrigger(
    val player: EntityPredicate?,
    val entity: EntityPredicate?,
    val source: DamageSourcePredicate?
) : Trigger {
    
    companion object : Adapter<KillMobNearSculkCatalystTrigger, KilledTrigger.TriggerInstance> {
        
        private val ID = ResourceLocation("kill_mob_near_sculk_catalyst")
        
        override fun toNMS(value: KillMobNearSculkCatalystTrigger): KilledTrigger.TriggerInstance {
            return KilledTrigger.TriggerInstance(
                ID,
                EntityPredicate.EntityPredicateCompositeAdapter.toNMS(value.player),
                EntityPredicate.EntityPredicateCompositeAdapter.toNMS(value.entity),
                DamageSourcePredicate.toNMS(value.source)
            )
        }
        
    }
    
    class Builder : Trigger.Builder<KillMobNearSculkCatalystTrigger>() {
        
        private var entity: EntityPredicate? = null
        private var source: DamageSourcePredicate? = null
        
        fun entity(init: EntityPredicate.Builder.() -> Unit) {
            entity = EntityPredicate.Builder().apply(init).build()
        }
        
        fun source(init: DamageSourcePredicate.Builder.() -> Unit) {
            source = DamageSourcePredicate.Builder().apply(init).build()
        }
        
        override fun build(): KillMobNearSculkCatalystTrigger {
            return KillMobNearSculkCatalystTrigger(player, entity, source)
        }
        
    }
    
}