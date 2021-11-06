package xyz.xenondevs.nova.command.impl

import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import xyz.xenondevs.nova.command.Command
import xyz.xenondevs.nova.command.executesCatching
import xyz.xenondevs.nova.command.player
import xyz.xenondevs.nova.command.requiresPlayerPermission
import xyz.xenondevs.nova.data.recipe.RecipeContainer
import xyz.xenondevs.nova.data.recipe.RecipeRegistry
import xyz.xenondevs.nova.ui.menu.item.recipes.RecipesWindow
import xyz.xenondevs.nova.ui.menu.item.recipes.craftingtype.RecipeType

object NovaRecipeCommand : Command("nvrecipe") {
    
    init {
        builder = builder
            .requiresPlayerPermission("nova.command.nvrecipe")
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