package xyz.xenondevs.nova.command.impl

import com.mojang.brigadier.context.CommandContext
import net.md_5.bungee.api.ChatColor
import net.minecraft.commands.CommandSourceStack
import xyz.xenondevs.nova.command.*
import xyz.xenondevs.nova.data.recipe.RecipeContainer
import xyz.xenondevs.nova.data.recipe.RecipeRegistry
import xyz.xenondevs.nova.ui.menu.item.recipes.RecipesWindow
import xyz.xenondevs.nova.ui.menu.item.recipes.craftingtype.RecipeGroup
import xyz.xenondevs.nova.util.ItemUtils
import xyz.xenondevs.nova.util.data.localized

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
            .executesCatching { showCurrentRecipe(it) }
    }
    
    private fun showCurrentRecipe(ctx: CommandContext<CommandSourceStack>) {
        val player = ctx.player
        val item = player.inventory.itemInMainHand
        if (!item.type.isAir) {
            val recipes = RecipeRegistry.CREATION_RECIPES[ItemUtils.getNameKey(item)]
            if (recipes != null) RecipesWindow(player, recipes).show()
            else ctx.source.sendFailure(localized(ChatColor.RED, "command.nova.recipe.no-recipe"))
        } else ctx.source.sendFailure(localized(ChatColor.RED, "command.nova.no-item-in-hand"))
    }
    
    private fun showRecipe(recipes: Map<RecipeGroup, List<RecipeContainer>>, ctx: CommandContext<CommandSourceStack>) {
        RecipesWindow(ctx.player, recipes).show()
    }
    
}