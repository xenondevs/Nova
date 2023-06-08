package xyz.xenondevs.nmsutils.advancement.trigger

import net.minecraft.advancements.critereon.SlideDownBlockTrigger
import net.minecraft.advancements.critereon.StatePropertiesPredicate
import net.minecraft.world.level.block.Block
import org.bukkit.Material
import xyz.xenondevs.nmsutils.advancement.predicate.StatePropertiesPredicateBuilder
import xyz.xenondevs.nmsutils.internal.util.nmsBlock

class SlideDownBlockTriggerBuilder : TriggerBuilder<SlideDownBlockTrigger.TriggerInstance>() {
    
    private var block: Block? = null
    private var properties = StatePropertiesPredicate.ANY
    
    fun block(block: Block) {
        this.block = block
    }
    
    fun block(block: Material) {
        this.block = block.nmsBlock
    }
    
    fun properties(init: StatePropertiesPredicateBuilder.() -> Unit) {
        this.properties = StatePropertiesPredicateBuilder().apply(init).build()
    }
    
    override fun build() = SlideDownBlockTrigger.TriggerInstance(player, block, properties)
    
}