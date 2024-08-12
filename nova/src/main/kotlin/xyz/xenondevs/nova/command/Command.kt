@file:Suppress("DeprecatedCallableAddReplaceWith", "UnstableApiUsage")

package xyz.xenondevs.nova.command

import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.entity.Player

internal val CommandContext<CommandSourceStack>.player: Player
    get() = source.sender as Player

internal inline operator fun <reified V> CommandContext<*>.get(name: String): V =
    getArgument(name, V::class.java)

internal fun <C, T : ArgumentBuilder<C, T>> ArgumentBuilder<C, T>.executes0(run: (CommandContext<C>) -> Unit): T =
    executes { run(it); 0 }

internal fun LiteralArgumentBuilder<CommandSourceStack>.requiresConsole(): LiteralArgumentBuilder<CommandSourceStack> =
    this.requires { it.sender is ConsoleCommandSender }

internal fun LiteralArgumentBuilder<CommandSourceStack>.requiresPlayer(): LiteralArgumentBuilder<CommandSourceStack> =
    this.requires { it.sender is Player }

internal fun LiteralArgumentBuilder<CommandSourceStack>.requiresPermission(permission: String): LiteralArgumentBuilder<CommandSourceStack> =
    this.requires { it.sender.hasPermission(permission) }

internal abstract class Command {
    
    abstract val node: LiteralCommandNode<CommandSourceStack>
    
}
