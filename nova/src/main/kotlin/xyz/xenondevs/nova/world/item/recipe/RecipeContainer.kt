package xyz.xenondevs.nova.world.item.recipe

import net.kyori.adventure.key.Key
import org.bukkit.Keyed

class RecipeContainer(val recipe: Any) {
    
    private val id: Key = when (recipe) {
        is Keyed -> recipe.key
        is NovaRecipe -> recipe.id
        else -> throw IllegalArgumentException("Could not find a recipe key")
    }
    
    val type = RecipeType.of(recipe)
    
    override fun equals(other: Any?): Boolean {
        return other is RecipeContainer && id == other.id
    }
    
    override fun hashCode(): Int =
        id.hashCode()
    
}