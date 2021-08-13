package xyz.xenondevs.nova.command.impl

import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import xyz.xenondevs.nova.command.PlayerCommand
import xyz.xenondevs.nova.command.executesCatching
import xyz.xenondevs.nova.command.player
import xyz.xenondevs.nova.data.recipe.RecipeContainer
import xyz.xenondevs.nova.data.recipe.RecipeRegistry
import xyz.xenondevs.nova.ui.menu.recipes.RecipesWindow
import xyz.xenondevs.nova.ui.menu.recipes.craftingtype.RecipeType

object NovaRecipeCommand : PlayerCommand("nvrecipe") {
    
    init {
        builder = builder
            .apply {
                RecipeRegistry.CREATION_RECIPES.forEach { (key, recipes) ->
                    then(literal(key)
                        .executesCatching { showRecipe(recipes, it) }
                    )
                }
            }
    }
    
    private fun showRecipe(recipes: Map<RecipeType, List<RecipeContainer>>, context: CommandContext<CommandSourceStack>) {
        RecipesWindow(context.player, recipes).show()
    }
    
}