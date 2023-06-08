package xyz.xenondevs.nmsutils.advancement.trigger

import net.minecraft.advancements.critereon.ContextAwarePredicate
import net.minecraft.advancements.critereon.KilledByCrossbowTrigger
import net.minecraft.advancements.critereon.MinMaxBounds
import xyz.xenondevs.nmsutils.advancement.predicate.EntityPredicateBuilder
import xyz.xenondevs.nmsutils.internal.util.asContextAwarePredicate

class KilledByCrossbowTriggerBuilder : TriggerBuilder<KilledByCrossbowTrigger.TriggerInstance>() {
    
    private var uniqueEntityTypes = MinMaxBounds.Ints.ANY
    private val victims = ArrayList<ContextAwarePredicate>()
    
    fun uniqueEntityTypes(uniqueEntityTypes: MinMaxBounds.Ints) {
        this.uniqueEntityTypes = uniqueEntityTypes
    }
    
    fun uniqueEntityTypes(uniqueEntityTypes: IntRange) {
        this.uniqueEntityTypes = MinMaxBounds.Ints.between(uniqueEntityTypes.first, uniqueEntityTypes.last)
    }
    
    fun uniqueEntityTypes(uniqueEntityTypes: Int) {
        this.uniqueEntityTypes = MinMaxBounds.Ints.exactly(uniqueEntityTypes)
    }
    
    fun victim(init: EntityPredicateBuilder.() -> Unit) {
        victims += EntityPredicateBuilder().apply(init).build().asContextAwarePredicate()
    }
    
    override fun build(): KilledByCrossbowTrigger.TriggerInstance {
        return KilledByCrossbowTrigger.TriggerInstance(player, victims.toTypedArray(), uniqueEntityTypes)
    }
    
}