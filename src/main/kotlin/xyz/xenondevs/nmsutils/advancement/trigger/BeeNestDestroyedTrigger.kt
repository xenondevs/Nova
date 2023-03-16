package xyz.xenondevs.nmsutils.advancement.trigger

import org.bukkit.Material
import org.bukkit.craftbukkit.v1_19_R3.util.CraftMagicNumbers
import xyz.xenondevs.nmsutils.adapter.Adapter
import xyz.xenondevs.nmsutils.adapter.impl.IntBoundsAdapter
import xyz.xenondevs.nmsutils.advancement.predicate.EntityPredicate
import xyz.xenondevs.nmsutils.advancement.predicate.ItemPredicate
import net.minecraft.advancements.critereon.BeeNestDestroyedTrigger as MojangBeeNestDestroyedTrigger

class BeeNestDestroyedTrigger(
    val player: EntityPredicate?,
    val block: Material?,
    val item: ItemPredicate?,
    val beesInside: IntRange?
) : Trigger {
    
    companion object : Adapter<BeeNestDestroyedTrigger, MojangBeeNestDestroyedTrigger.TriggerInstance> {
        
        override fun toNMS(value: BeeNestDestroyedTrigger): MojangBeeNestDestroyedTrigger.TriggerInstance {
            return MojangBeeNestDestroyedTrigger.TriggerInstance(
                EntityPredicate.EntityPredicateCompositeAdapter.toNMS(value.player),
                CraftMagicNumbers.getBlock(value.block),
                ItemPredicate.toNMS(value.item),
                IntBoundsAdapter.toNMS(value.beesInside)
            )
        }
        
    }
    
    class Builder : Trigger.Builder<BeeNestDestroyedTrigger>() {
        
        private var block: Material? = null
        private var item: ItemPredicate? = null
        private var beesInside: IntRange? = null
        
        fun block(block: Material) {
            this.block = block
        }
        
        fun item(init: ItemPredicate.Builder.() -> Unit) {
            this.item = ItemPredicate.Builder().apply(init).build()
        }
        
        fun beesInside(beesInside: IntRange) {
            this.beesInside = beesInside
        }
        
        override fun build(): BeeNestDestroyedTrigger {
            return BeeNestDestroyedTrigger(player, block, item, beesInside)
        }
        
    }
    
}