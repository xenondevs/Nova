package xyz.xenondevs.nova.command.impl

import com.mojang.brigadier.context.CommandContext
import net.md_5.bungee.api.ChatColor
import net.minecraft.commands.CommandSourceStack
import xyz.xenondevs.nova.command.*
import xyz.xenondevs.nova.data.config.GlobalValues
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.util.data.localized

object NovaConfigCommand : Command("nvconfig") {
    
    init {
        builder = builder
            .requiresPlayerPermission("nova.config")
            .then(literal("reload").executesCatching { reload(it) })
    }
    
    fun reload(ctx: CommandContext<CommandSourceStack>) {
        ctx.source.sendSuccess(localized(ChatColor.GRAY, "command.nvconfig.reload.start"))
        NovaConfig.reload()
        ctx.source.sendSuccess(localized(ChatColor.GRAY, "command.nvconfig.reload.end"))
    }
    
}