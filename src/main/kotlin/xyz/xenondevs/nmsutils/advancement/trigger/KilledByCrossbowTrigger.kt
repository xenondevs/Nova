package xyz.xenondevs.nmsutils.advancement.trigger

import xyz.xenondevs.nmsutils.adapter.Adapter
import xyz.xenondevs.nmsutils.adapter.impl.IntBoundsAdapter
import xyz.xenondevs.nmsutils.advancement.predicate.EntityPredicate
import xyz.xenondevs.nmsutils.util.mapToArray
import net.minecraft.advancements.critereon.KilledByCrossbowTrigger as MojangKilledByCrossbowTrigger

class KilledByCrossbowTrigger(
    val player: EntityPredicate?,
    val uniqueEntityTypes: IntRange?,
    val victims: List<EntityPredicate>?
) : Trigger {
    
    companion object : Adapter<KilledByCrossbowTrigger, MojangKilledByCrossbowTrigger.TriggerInstance> {
        
        override fun toNMS(value: KilledByCrossbowTrigger): MojangKilledByCrossbowTrigger.TriggerInstance {
            return MojangKilledByCrossbowTrigger.TriggerInstance(
                EntityPredicate.EntityPredicateCompositeAdapter.toNMS(value.player),
                value.victims?.mapToArray(EntityPredicate.EntityPredicateCompositeAdapter::toNMS) ?: emptyArray(),
                IntBoundsAdapter.toNMS(value.uniqueEntityTypes)
            )
        }
        
    }
    
    class Builder : Trigger.Builder<KilledByCrossbowTrigger>() {
        
        private var uniqueEntityTypes: IntRange? = null
        private val victims = ArrayList<EntityPredicate>()
        
        fun uniqueEntityTypes(uniqueEntityTypes: IntRange) {
            this.uniqueEntityTypes = uniqueEntityTypes
        }
        
        fun victim(init: EntityPredicate.Builder.() -> Unit) {
            victims += EntityPredicate.Builder().apply(init).build()
        }
        
        override fun build(): KilledByCrossbowTrigger {
            return KilledByCrossbowTrigger(player, uniqueEntityTypes, victims.takeUnless(List<*>::isEmpty))
        }
        
    }
    
}