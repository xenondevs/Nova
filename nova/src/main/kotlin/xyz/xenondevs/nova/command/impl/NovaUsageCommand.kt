@file:Suppress("UnstableApiUsage")

package xyz.xenondevs.nova.command.impl

import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands.argument
import io.papermc.paper.command.brigadier.Commands.literal
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import xyz.xenondevs.nova.command.Command
import xyz.xenondevs.nova.command.argument.UsageRecipeArgumentType
import xyz.xenondevs.nova.command.executes0
import xyz.xenondevs.nova.command.get
import xyz.xenondevs.nova.command.player
import xyz.xenondevs.nova.command.requiresPermission
import xyz.xenondevs.nova.command.requiresPlayer
import xyz.xenondevs.nova.ui.menu.explorer.recipes.showRecipes

internal object NovaUsageCommand : Command() {
    
    override val node: LiteralCommandNode<CommandSourceStack> = literal("nvusage")
        .requiresPlayer()
        .requiresPermission("nova.command.nvusage")
        .executes0(::showUsage)
        .then(argument("recipe", UsageRecipeArgumentType)
            .executes0(::showCurrentUsage))
        .build()
    
    private fun showUsage(ctx: CommandContext<CommandSourceStack>) {
        val player = ctx.player
        val recipe: String = ctx["recipe"]
        player.showRecipes(recipe)
    }
    
    private fun showCurrentUsage(ctx: CommandContext<CommandSourceStack>) {
        val player = ctx.player
        val item = player.inventory.itemInMainHand
        if (!item.type.isAir) {
            if (!player.showRecipes(item)) {
                ctx.source.sender.sendMessage(Component.translatable("command.nova.usage.no-usage", NamedTextColor.RED))
            }
        } else ctx.source.sender.sendMessage(Component.translatable("command.nova.no-item-in-hand", NamedTextColor.RED))
    }
    
}