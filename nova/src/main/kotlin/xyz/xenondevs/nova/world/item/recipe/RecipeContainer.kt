package xyz.xenondevs.nova.world.item.recipe

import net.minecraft.resources.ResourceLocation
import org.bukkit.Keyed
import xyz.xenondevs.nova.util.resourceLocation

class RecipeContainer(val recipe: Any) {
    
    private val id: ResourceLocation = when (recipe) {
        is Keyed -> recipe.key.resourceLocation
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