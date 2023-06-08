package xyz.xenondevs.nmsutils.advancement.trigger

import net.minecraft.advancements.critereon.ItemPredicate
import net.minecraft.advancements.critereon.RecipeCraftedTrigger
import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.nmsutils.advancement.AdvancementDsl
import xyz.xenondevs.nmsutils.advancement.predicate.ItemPredicateBuilder

class RecipeCraftedTriggerBuilder : TriggerBuilder<RecipeCraftedTrigger.TriggerInstance>() {
    
    private var id: ResourceLocation? = null
    private var ingredients: List<ItemPredicate> = emptyList()
    
    fun ingredients(init: IngredientsBuilder.() -> Unit) {
        ingredients = IngredientsBuilder().apply(init).build()
    }
    
    override fun build() = RecipeCraftedTrigger.TriggerInstance(player, id, ingredients)
    
}

@AdvancementDsl
class IngredientsBuilder {
    
    private val ingredients = ArrayList<ItemPredicate>()
    
    fun ingredient(init: ItemPredicateBuilder.() -> Unit) {
        ingredients += ItemPredicateBuilder().apply(init).build()
    }
    
    internal fun build(): List<ItemPredicate> = ingredients
    
}