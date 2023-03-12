package xyz.xenondevs.nova.command.impl

import com.mojang.brigadier.context.CommandContext
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.commands.CommandSourceStack
import xyz.xenondevs.nova.command.Command
import xyz.xenondevs.nova.command.executesCatching
import xyz.xenondevs.nova.command.player
import xyz.xenondevs.nova.command.requiresPlayerPermission
import xyz.xenondevs.nova.command.sendFailure
import xyz.xenondevs.nova.data.recipe.RecipeRegistry
import xyz.xenondevs.nova.ui.menu.item.recipes.showRecipes

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
                ctx.source.sendFailure(Component.translatable("command.nova.recipe.no-recipe", NamedTextColor.RED))
        } else ctx.source.sendFailure(Component.translatable("command.nova.no-item-in-hand", NamedTextColor.RED))
    }
    
}