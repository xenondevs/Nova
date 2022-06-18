package xyz.xenondevs.nova.command.impl

import com.mojang.brigadier.context.CommandContext
import net.md_5.bungee.api.ChatColor
import net.minecraft.commands.CommandSourceStack
import xyz.xenondevs.nova.command.*
import xyz.xenondevs.nova.data.recipe.RecipeRegistry
import xyz.xenondevs.nova.ui.menu.item.recipes.showRecipes
import xyz.xenondevs.nova.util.data.localized

internal object NovaRecipeCommand : Command("nvrecipe") {
    
    init {
        builder = builder
            .requiresPlayerPermission("nova.command.nvrecipe")
            .apply {
                (RecipeRegistry.CREATION_RECIPES.keys + RecipeRegistry.creationInfo.keys).forEach { id ->
                    then(literal(id).executesCatching { it.player.showRecipes(id) })
                }
            }
            .executesCatching { showCurrentRecipe(it) }
    }
    
    private fun showCurrentRecipe(ctx: CommandContext<CommandSourceStack>) {
        val player = ctx.player
        val item = player.inventory.itemInMainHand
        if (!item.type.isAir) {
            if (!player.showRecipes(item))
                ctx.source.sendFailure(localized(ChatColor.RED, "command.nova.recipe.no-recipe"))
        } else ctx.source.sendFailure(localized(ChatColor.RED, "command.nova.no-item-in-hand"))
    }
    
}