package xyz.xenondevs.nmsutils.advancement.trigger

import xyz.xenondevs.nmsutils.adapter.Adapter
import xyz.xenondevs.nmsutils.advancement.predicate.EntityPredicate
import xyz.xenondevs.nmsutils.advancement.predicate.ItemPredicate
import net.minecraft.advancements.critereon.FishingRodHookedTrigger as MojangFishingRodHookedTrigger

class FishingRodHookedTrigger(
    val player: EntityPredicate?,
    val entity: EntityPredicate?,
    val item: ItemPredicate?,
    val rod: ItemPredicate?
) : Trigger {
    
    companion object : Adapter<FishingRodHookedTrigger, MojangFishingRodHookedTrigger.TriggerInstance> {
        
        override fun toNMS(value: FishingRodHookedTrigger): MojangFishingRodHookedTrigger.TriggerInstance {
            return MojangFishingRodHookedTrigger.TriggerInstance(
                EntityPredicate.EntityPredicateCompositeAdapter.toNMS(value.player),
                ItemPredicate.toNMS(value.rod),
                EntityPredicate.EntityPredicateCompositeAdapter.toNMS(value.entity),
                ItemPredicate.toNMS(value.item)
            )
        }
        
    }
    
    class Builder : Trigger.Builder<FishingRodHookedTrigger>() {
        
        private var entity: EntityPredicate? = null
        private var item: ItemPredicate? = null
        private var rod: ItemPredicate? = null
        
        fun entity(init: EntityPredicate.Builder.() -> Unit) {
            entity = EntityPredicate.Builder().apply(init).build()
        }
        
        fun item(init: ItemPredicate.Builder.() -> Unit) {
            item = ItemPredicate.Builder().apply(init).build()
        }
        
        fun rod(init: ItemPredicate.Builder.() -> Unit) {
            rod = ItemPredicate.Builder().apply(init).build()
        }
        
        override fun build(): FishingRodHookedTrigger {
            return FishingRodHookedTrigger(player, entity, item, rod)
        }
        
    }
    
}