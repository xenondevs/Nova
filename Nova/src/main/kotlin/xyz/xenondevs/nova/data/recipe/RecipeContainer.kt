package xyz.xenondevs.nova.data.recipe

import org.bukkit.Keyed
import org.bukkit.inventory.Recipe

class RecipeContainer(val recipe: Any) {
    
    private val key = when (recipe) {
        is Keyed -> recipe.key
        is NovaRecipe -> recipe.key
        else -> throw IllegalArgumentException("Could not find a recipe key")
    }
    
    val result = if (recipe is Recipe) recipe.result else (recipe as NovaRecipe).result
    val type = RecipeType.of(recipe)
    
    override fun equals(other: Any?): Boolean {
        return other is RecipeContainer && key == other.key
    }
    
    override fun hashCode(): Int =
        key.hashCode()
    
}