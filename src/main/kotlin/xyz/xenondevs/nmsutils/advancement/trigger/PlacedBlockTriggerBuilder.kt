package xyz.xenondevs.nmsutils.advancement.trigger

import net.minecraft.advancements.CriteriaTriggers
import net.minecraft.advancements.critereon.ContextAwarePredicate
import net.minecraft.advancements.critereon.ItemPredicate
import net.minecraft.advancements.critereon.ItemUsedOnLocationTrigger
import net.minecraft.advancements.critereon.LocationPredicate
import net.minecraft.advancements.critereon.StatePropertiesPredicate
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition
import net.minecraft.world.level.storage.loot.predicates.MatchTool
import org.bukkit.Material
import xyz.xenondevs.nmsutils.LocationCheck
import xyz.xenondevs.nmsutils.LootItemBlockStatePropertyCondition
import xyz.xenondevs.nmsutils.advancement.predicate.ItemPredicateBuilder
import xyz.xenondevs.nmsutils.advancement.predicate.LocationPredicateBuilder
import xyz.xenondevs.nmsutils.advancement.predicate.StatePropertiesPredicateBuilder
import xyz.xenondevs.nmsutils.internal.util.nmsBlock

class PlacedBlockTriggerBuilder : TriggerBuilder<ItemUsedOnLocationTrigger.TriggerInstance>() {
    
    private var block: Block? = null
    private var item = ItemPredicate.ANY
    private var location = LocationPredicate.ANY
    private var state = StatePropertiesPredicate.ANY
    
    fun block(block: Block) {
        this.block = block
    }
    
    fun block(block: Material) {
        this.block = block.nmsBlock
    }
    
    fun item(init: ItemPredicateBuilder.() -> Unit) {
        this.item = ItemPredicateBuilder().apply(init).build()
    }
    
    fun location(init: LocationPredicateBuilder.() -> Unit) {
        this.location = LocationPredicateBuilder().apply(init).build()
    }
    
    fun state(init: StatePropertiesPredicateBuilder.() -> Unit) {
        this.state = StatePropertiesPredicateBuilder().apply(init).build()
    }
    
    override fun build(): ItemUsedOnLocationTrigger.TriggerInstance {
        require(state == StatePropertiesPredicate.ANY || block != null) { "Block must be set when using state properties" }
        
        val conditions = ArrayList<LootItemCondition>()
        conditions += LocationCheck(location)
        conditions += MatchTool(item)
        if (block != null)
            conditions += LootItemBlockStatePropertyCondition(block!!, state)
        
        return ItemUsedOnLocationTrigger.TriggerInstance(
            CriteriaTriggers.PLACED_BLOCK.id,
            player,
            ContextAwarePredicate.create(*conditions.toTypedArray())
        )
    }
    
}