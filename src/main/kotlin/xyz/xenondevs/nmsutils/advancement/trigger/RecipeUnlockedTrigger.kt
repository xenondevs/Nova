package xyz.xenondevs.nmsutils.advancement.trigger

import xyz.xenondevs.nmsutils.adapter.Adapter
import xyz.xenondevs.nmsutils.advancement.predicate.EntityPredicate
import xyz.xenondevs.nmsutils.util.resourceLocation
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger as MojangRecipeUnlockedTrigger

class RecipeUnlockedTrigger(
    val player: EntityPredicate?,
    val recipe: String
) : Trigger {
    
    companion object : Adapter<RecipeUnlockedTrigger, MojangRecipeUnlockedTrigger.TriggerInstance> {
        
        override fun toNMS(value: RecipeUnlockedTrigger): MojangRecipeUnlockedTrigger.TriggerInstance {
            return MojangRecipeUnlockedTrigger.TriggerInstance(
                EntityPredicate.EntityPredicateCompositeAdapter.toNMS(value.player),
                value.recipe.resourceLocation!!
            )
        }
        
    }
    
    class Builder : Trigger.Builder<RecipeUnlockedTrigger>() {
        
        private var recipe: String? = null
        
        fun recipe(recipe: String) {
            this.recipe = recipe
        }
        
        override fun build(): RecipeUnlockedTrigger {
            checkNotNull(recipe) { "Recipe is not set" }
            return RecipeUnlockedTrigger(player, recipe!!)
        }
        
    }
    
}