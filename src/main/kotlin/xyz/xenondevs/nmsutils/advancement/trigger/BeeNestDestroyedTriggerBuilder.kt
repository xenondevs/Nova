package xyz.xenondevs.nmsutils.advancement.trigger

import net.minecraft.advancements.critereon.BeeNestDestroyedTrigger
import net.minecraft.advancements.critereon.ItemPredicate
import net.minecraft.advancements.critereon.MinMaxBounds
import net.minecraft.world.level.block.Block
import org.bukkit.Material
import xyz.xenondevs.nmsutils.advancement.predicate.ItemPredicateBuilder
import xyz.xenondevs.nmsutils.internal.util.nmsBlock

class BeeNestDestroyedTriggerBuilder : TriggerBuilder<BeeNestDestroyedTrigger.TriggerInstance>() {
    
    private var block: Block? = null
    private var item = ItemPredicate.ANY
    private var beesInside = MinMaxBounds.Ints.ANY
    
    fun block(block: Block) {
        this.block = block
    }
    
    fun block(block: Material) {
        this.block = block.nmsBlock
    }
    
    fun item(init: ItemPredicateBuilder.() -> Unit) {
        this.item = ItemPredicateBuilder().apply(init).build()
    }
    
    fun beesInside(beesInside: MinMaxBounds.Ints) {
        this.beesInside = beesInside
    }
    
    fun beesInside(beesInside: IntRange) {
        this.beesInside = MinMaxBounds.Ints.between(beesInside.first, beesInside.last)
    }
    
    fun beesInside(beesInside: Int) {
        this.beesInside = MinMaxBounds.Ints.exactly(beesInside)
    }
    
    override fun build(): BeeNestDestroyedTrigger.TriggerInstance {
        return BeeNestDestroyedTrigger.TriggerInstance(player, block, item, beesInside)
    }
    
}