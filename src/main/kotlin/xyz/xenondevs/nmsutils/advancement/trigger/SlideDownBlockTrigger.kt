package xyz.xenondevs.nmsutils.advancement.trigger

import org.bukkit.Material
import org.bukkit.craftbukkit.v1_19_R1.util.CraftMagicNumbers
import xyz.xenondevs.nmsutils.adapter.Adapter
import xyz.xenondevs.nmsutils.advancement.predicate.EntityPredicate
import xyz.xenondevs.nmsutils.advancement.predicate.StatePropertiesPredicate
import net.minecraft.advancements.critereon.SlideDownBlockTrigger as MojangSlideDownBlockTrigger

class SlideDownBlockTrigger(
    val player: EntityPredicate?,
    val block: Material?,
    val properties: StatePropertiesPredicate?,
) : Trigger {
    
    companion object : Adapter<SlideDownBlockTrigger, MojangSlideDownBlockTrigger.TriggerInstance> {
        
        override fun toNMS(value: SlideDownBlockTrigger): MojangSlideDownBlockTrigger.TriggerInstance {
            return MojangSlideDownBlockTrigger.TriggerInstance(
                EntityPredicate.EntityPredicateCompositeAdapter.toNMS(value.player),
                CraftMagicNumbers.getBlock(value.block),
                StatePropertiesPredicate.toNMS(value.properties)
            )
        }
        
    }
    
    class Builder : Trigger.Builder<SlideDownBlockTrigger>() {
        
        var block: Material? = null
        var properties: StatePropertiesPredicate? = null
        
        fun block(block: Material) {
            this.block = block
        }
        
        fun properties(init: StatePropertiesPredicate.Builder.() -> Unit) {
            this.properties = StatePropertiesPredicate.Builder().apply(init).build()
        }
        
        override fun build(): SlideDownBlockTrigger {
            return SlideDownBlockTrigger(player, block, properties)
        }
        
    }
    
}