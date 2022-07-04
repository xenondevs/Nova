package xyz.xenondevs.nmsutils.advancement.trigger

import net.minecraft.advancements.critereon.ItemDurabilityTrigger
import xyz.xenondevs.nmsutils.adapter.Adapter
import xyz.xenondevs.nmsutils.adapter.impl.IntBoundsAdapter
import xyz.xenondevs.nmsutils.advancement.predicate.EntityPredicate
import xyz.xenondevs.nmsutils.advancement.predicate.ItemPredicate

class ItemDurabilityChangedTrigger(
    val player: EntityPredicate?,
    val delta: IntRange?,
    val durability: IntRange?,
    val item: ItemPredicate?
) : Trigger {
    
    companion object : Adapter<ItemDurabilityChangedTrigger, ItemDurabilityTrigger.TriggerInstance> {
        
        override fun toNMS(value: ItemDurabilityChangedTrigger): ItemDurabilityTrigger.TriggerInstance {
            return ItemDurabilityTrigger.TriggerInstance(
                EntityPredicate.EntityPredicateCompositeAdapter.toNMS(value.player),
                ItemPredicate.toNMS(value.item),
                IntBoundsAdapter.toNMS(value.durability),
                IntBoundsAdapter.toNMS(value.delta)
            )
        }
        
    }
    
    class Builder : Trigger.Builder<ItemDurabilityChangedTrigger>() {
        
        private var delta: IntRange? = null
        private var durability: IntRange? = null
        private var item: ItemPredicate? = null
        
        fun delta(delta: IntRange) {
            this.delta = delta
        }
        
        fun durability(durability: IntRange) {
            this.durability = durability
        }
        
        fun item(init: ItemPredicate.Builder.() -> Unit) {
            this.item = ItemPredicate.Builder().apply(init).build()
        }
        
        override fun build(): ItemDurabilityChangedTrigger {
            return ItemDurabilityChangedTrigger(player, delta, durability, item)
        }
        
    }
    
}