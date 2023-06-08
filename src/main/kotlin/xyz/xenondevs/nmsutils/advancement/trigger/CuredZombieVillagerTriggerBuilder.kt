package xyz.xenondevs.nmsutils.advancement.trigger

import net.minecraft.advancements.critereon.ContextAwarePredicate
import net.minecraft.advancements.critereon.CuredZombieVillagerTrigger
import xyz.xenondevs.nmsutils.advancement.predicate.EntityPredicateBuilder
import xyz.xenondevs.nmsutils.internal.util.asContextAwarePredicate

class CuredZombieVillagerTriggerBuilder : TriggerBuilder<CuredZombieVillagerTrigger.TriggerInstance>() {
    
    private var villager = ContextAwarePredicate.ANY
    private var zombie = ContextAwarePredicate.ANY
    
    fun villager(init: EntityPredicateBuilder.() -> Unit) {
        villager = EntityPredicateBuilder().apply(init).build().asContextAwarePredicate()
    }
    
    fun zombie(zombie: EntityPredicateBuilder.() -> Unit) {
        this.zombie = EntityPredicateBuilder().apply(zombie).build().asContextAwarePredicate()
    }
    
    override fun build(): CuredZombieVillagerTrigger.TriggerInstance {
        return CuredZombieVillagerTrigger.TriggerInstance(player, villager, zombie)
    }
    
}