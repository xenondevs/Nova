package xyz.xenondevs.nmsutils.advancement.trigger

import org.bukkit.Material
import org.bukkit.craftbukkit.v1_19_R3.util.CraftMagicNumbers
import xyz.xenondevs.nmsutils.adapter.Adapter
import xyz.xenondevs.nmsutils.advancement.predicate.EntityPredicate
import xyz.xenondevs.nmsutils.advancement.predicate.StatePropertiesPredicate
import net.minecraft.advancements.critereon.EnterBlockTrigger as MojangEnterBlockTrigger

class EnterBlockTrigger(
    val player: EntityPredicate?,
    val block: Material?,
    val properties: StatePropertiesPredicate?
) : Trigger {
    
    companion object : Adapter<EnterBlockTrigger, MojangEnterBlockTrigger.TriggerInstance> {
        
        override fun toNMS(value: EnterBlockTrigger): MojangEnterBlockTrigger.TriggerInstance {
            return MojangEnterBlockTrigger.TriggerInstance(
                EntityPredicate.EntityPredicateCompositeAdapter.toNMS(value.player),
                CraftMagicNumbers.getBlock(value.block),
                StatePropertiesPredicate.toNMS(value.properties)
            )
        }
        
    }
    
    class Builder : Trigger.Builder<EnterBlockTrigger>() {
        
        private var block: Material? = null
        private var properties: StatePropertiesPredicate? = null
        
        fun block(block: Material) {
            this.block = block
        }
        
        fun properties(init: StatePropertiesPredicate.Builder.() -> Unit) {
            this.properties = StatePropertiesPredicate.Builder().apply(init).build()
        }
        
        override fun build(): EnterBlockTrigger {
            return EnterBlockTrigger(player, block, properties)
        }
        
    }
    
}