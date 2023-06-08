package xyz.xenondevs.nmsutils.advancement.trigger

import net.minecraft.advancements.CriteriaTriggers
import net.minecraft.advancements.critereon.ContextAwarePredicate
import net.minecraft.advancements.critereon.ItemPredicate
import net.minecraft.advancements.critereon.ItemUsedOnLocationTrigger
import net.minecraft.advancements.critereon.LocationPredicate
import net.minecraft.core.BlockPos
import net.minecraft.world.level.storage.loot.predicates.MatchTool
import xyz.xenondevs.nmsutils.LocationCheck
import xyz.xenondevs.nmsutils.advancement.predicate.ItemPredicateBuilder
import xyz.xenondevs.nmsutils.advancement.predicate.LocationPredicateBuilder

class ItemUsedOnBlockTriggerBuilder : TriggerBuilder<ItemUsedOnLocationTrigger.TriggerInstance>() {
    
    private var location = LocationPredicate.ANY
    private var item = ItemPredicate.ANY
    
    fun location(init: LocationPredicateBuilder.() -> Unit) {
        location = LocationPredicateBuilder().apply(init).build()
    }
    
    fun item(init: ItemPredicateBuilder.() -> Unit) {
        item = ItemPredicateBuilder().apply(init).build()
    }
    
    override fun build(): ItemUsedOnLocationTrigger.TriggerInstance {
        return ItemUsedOnLocationTrigger.TriggerInstance(
            CriteriaTriggers.ITEM_USED_ON_BLOCK.id,
            player,
            ContextAwarePredicate.create(
                LocationCheck(location, BlockPos.ZERO),
                MatchTool(item)
            )
        )
    }
    
}