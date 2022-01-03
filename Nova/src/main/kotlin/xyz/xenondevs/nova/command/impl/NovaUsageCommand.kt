package xyz.xenondevs.nova.command.impl

import com.mojang.brigadier.context.CommandContext
import net.md_5.bungee.api.ChatColor
import net.minecraft.commands.CommandSourceStack
import xyz.xenondevs.nova.command.*
import xyz.xenondevs.nova.data.recipe.RecipeRegistry
import xyz.xenondevs.nova.ui.menu.item.recipes.showUsages
import xyz.xenondevs.nova.util.data.localized

object NovaUsageCommand : Command("nvusage") {
    
    init {
        builder = builder
            .requiresPlayerPermission("nova.command.nvusage")
            .apply {
                (RecipeRegistry.USAGE_RECIPES.keys + RecipeRegistry.USAGE_INFO.keys).forEach { id ->
                    then(literal(id)
                        .executesCatching { it.player.showUsages(id) }
                    )
                }
            }
            .executesCatching { showCurrentUsage(it) }
    }
    
    private fun showCurrentUsage(ctx: CommandContext<CommandSourceStack>) {
        val player = ctx.player
        val item = player.inventory.itemInMainHand
        if (!item.type.isAir) {
            if (!player.showUsages(item))
                ctx.source.sendFailure(localized(ChatColor.RED, "command.nova.usage.no-usage"))
        } else ctx.source.sendFailure(localized(ChatColor.RED, "command.nova.no-item-in-hand"))
    }
    
}