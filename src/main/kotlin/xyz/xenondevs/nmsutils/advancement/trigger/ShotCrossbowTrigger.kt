package xyz.xenondevs.nmsutils.advancement.trigger

import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.nmsutils.adapter.Adapter
import xyz.xenondevs.nmsutils.advancement.predicate.EntityPredicate
import xyz.xenondevs.nmsutils.advancement.predicate.ItemPredicate
import net.minecraft.advancements.critereon.ShotCrossbowTrigger as MojangShotCrossbowTrigger

class ShotCrossbowTrigger(
    val player: EntityPredicate?,
    val item: ItemPredicate?
) : Trigger {
    
    companion object : Adapter<ShotCrossbowTrigger, MojangShotCrossbowTrigger.TriggerInstance> {
        
        private val ID = ResourceLocation("ride_entity_in_lava")
        
        override fun toNMS(value: ShotCrossbowTrigger): MojangShotCrossbowTrigger.TriggerInstance {
            return MojangShotCrossbowTrigger.TriggerInstance(
                EntityPredicate.EntityPredicateCompositeAdapter.toNMS(value.player),
                ItemPredicate.toNMS(value.item)
            )
        }
        
    }
    
    class Builder : Trigger.Builder<ShotCrossbowTrigger>() {
        
        private var item: ItemPredicate? = null
        
        fun item(init: ItemPredicate.Builder.() -> Unit) {
            item = ItemPredicate.Builder().apply(init).build()
        }
        
        override fun build(): ShotCrossbowTrigger {
            return ShotCrossbowTrigger(player, item)
        }
        
    }
    
}