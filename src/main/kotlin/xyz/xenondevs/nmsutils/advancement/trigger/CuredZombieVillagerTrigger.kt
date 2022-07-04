package xyz.xenondevs.nmsutils.advancement.trigger

import xyz.xenondevs.nmsutils.adapter.Adapter
import xyz.xenondevs.nmsutils.advancement.predicate.EntityPredicate
import net.minecraft.advancements.critereon.CuredZombieVillagerTrigger as MojangCuredZombieVillagerTrigger

class CuredZombieVillagerTrigger(
    val player: EntityPredicate?,
    val villager: EntityPredicate?,
    val zombie: EntityPredicate?
) : Trigger {
    
    companion object : Adapter<CuredZombieVillagerTrigger, MojangCuredZombieVillagerTrigger.TriggerInstance> {
        
        override fun toNMS(value: CuredZombieVillagerTrigger): MojangCuredZombieVillagerTrigger.TriggerInstance {
            return MojangCuredZombieVillagerTrigger.TriggerInstance(
                EntityPredicate.EntityPredicateCompositeAdapter.toNMS(value.player),
                EntityPredicate.EntityPredicateCompositeAdapter.toNMS(value.zombie),
                EntityPredicate.EntityPredicateCompositeAdapter.toNMS(value.villager)
            )
        }
        
    }
    
    class Builder : Trigger.Builder<CuredZombieVillagerTrigger>() {
        
        private var villager: EntityPredicate? = null
        private var zombie: EntityPredicate? = null
        
        fun villager(init: EntityPredicate.Builder.() -> Unit) {
            villager = EntityPredicate.Builder().apply(init).build()
        }
        
        fun zombie(zombie: EntityPredicate.Builder.() -> Unit) {
            this.zombie = EntityPredicate.Builder().apply(zombie).build()
        }
        
        override fun build(): CuredZombieVillagerTrigger {
            return CuredZombieVillagerTrigger(player, villager, zombie)
        }
        
    }
    
}