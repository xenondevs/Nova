package xyz.xenondevs.nmsutils.advancement.trigger

import xyz.xenondevs.nmsutils.adapter.Adapter
import xyz.xenondevs.nmsutils.adapter.impl.IntBoundsAdapter
import xyz.xenondevs.nmsutils.advancement.predicate.EntityPredicate
import xyz.xenondevs.nmsutils.advancement.predicate.ItemPredicate
import net.minecraft.advancements.critereon.EnchantedItemTrigger as MojangEnchantedItemTrigger

class EnchantedItemTrigger(
    val player: EntityPredicate?,
    val item: ItemPredicate?,
    val levels: IntRange?
) : Trigger {
    
    companion object : Adapter<EnchantedItemTrigger, MojangEnchantedItemTrigger.TriggerInstance> {
        
        override fun toNMS(value: EnchantedItemTrigger): MojangEnchantedItemTrigger.TriggerInstance {
            return MojangEnchantedItemTrigger.TriggerInstance(
                EntityPredicate.EntityPredicateCompositeAdapter.toNMS(value.player),
                ItemPredicate.toNMS(value.item),
                IntBoundsAdapter.toNMS(value.levels)
            )
        }
        
    }
    
    class Builder : Trigger.Builder<EnchantedItemTrigger>() {
        
        private var item: ItemPredicate? = null
        private var levels: IntRange? = null
        
        fun item(init: ItemPredicate.Builder.() -> Unit) {
            item = ItemPredicate.Builder().apply(init).build()
        }
        
        fun levels(levels: IntRange) {
            this.levels = levels
        }
        
        override fun build(): EnchantedItemTrigger {
            return EnchantedItemTrigger(player, item, levels)
        }
        
    }
    
}