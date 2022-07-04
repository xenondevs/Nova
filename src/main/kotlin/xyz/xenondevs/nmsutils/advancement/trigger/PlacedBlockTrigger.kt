package xyz.xenondevs.nmsutils.advancement.trigger

import org.bukkit.Material
import org.bukkit.craftbukkit.v1_19_R1.util.CraftMagicNumbers
import xyz.xenondevs.nmsutils.adapter.Adapter
import xyz.xenondevs.nmsutils.advancement.predicate.EntityPredicate
import xyz.xenondevs.nmsutils.advancement.predicate.ItemPredicate
import xyz.xenondevs.nmsutils.advancement.predicate.LocationPredicate
import xyz.xenondevs.nmsutils.advancement.predicate.StatePropertiesPredicate
import net.minecraft.advancements.critereon.PlacedBlockTrigger as MojangPlacedBlockTrigger

class PlacedBlockTrigger(
    val player: EntityPredicate?,
    val block: Material?,
    val item: ItemPredicate?,
    val location: LocationPredicate?,
    val state: StatePropertiesPredicate?
) : Trigger {
    
    companion object : Adapter<PlacedBlockTrigger, MojangPlacedBlockTrigger.TriggerInstance> {
        
        override fun toNMS(value: PlacedBlockTrigger): MojangPlacedBlockTrigger.TriggerInstance {
            return MojangPlacedBlockTrigger.TriggerInstance(
                EntityPredicate.EntityPredicateCompositeAdapter.toNMS(value.player),
                CraftMagicNumbers.getBlock(value.block),
                StatePropertiesPredicate.toNMS(value.state),
                LocationPredicate.toNMS(value.location),
                ItemPredicate.toNMS(value.item)
            )
        }
        
    }
    
    class Builder : Trigger.Builder<PlacedBlockTrigger>() {
        
        private var block: Material? = null
        private var item: ItemPredicate? = null
        private var location: LocationPredicate? = null
        private var state: StatePropertiesPredicate? = null
        
        fun block(block: Material) {
            this.block = block
        }
        
        fun item(init: ItemPredicate.Builder.() -> Unit) {
            this.item = ItemPredicate.Builder().apply(init).build()
        }
        
        fun location(init: LocationPredicate.Builder.() -> Unit) {
            this.location = LocationPredicate.Builder().apply(init).build()
        }
        
        fun state(init: StatePropertiesPredicate.Builder.() -> Unit) {
            this.state = StatePropertiesPredicate.Builder().apply(init).build()
        }
        
        override fun build(): PlacedBlockTrigger {
            return PlacedBlockTrigger(player, block, item, location, state)
        }
        
    }
    
}