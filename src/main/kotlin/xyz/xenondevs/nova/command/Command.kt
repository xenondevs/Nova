package xyz.xenondevs.nova.command

import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSource
import net.minecraft.commands.CommandSourceStack
import net.minecraft.server.dedicated.DedicatedServer
import net.minecraft.server.rcon.RconConsoleSource
import org.bukkit.entity.Player

val CommandContext<CommandSourceStack>.player: Player
    get() = source.player

val CommandSourceStack.player: Player
    get() = playerOrException.bukkitEntity

fun CommandSource.isConsole() = this is DedicatedServer || this is RconConsoleSource

inline operator fun <reified V> CommandContext<*>.get(name: String): V =
    getArgument(name, V::class.java)

fun <CommandSourceStack, T : ArgumentBuilder<CommandSourceStack, T>> ArgumentBuilder<CommandSourceStack, T>.executesCatching(run: (CommandContext<CommandSourceStack>) -> Unit): T {
    return executes {
        try {
            run(it)
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
        
        0
    }
}

fun LiteralArgumentBuilder<CommandSourceStack>.requiresPermission(permission: String): LiteralArgumentBuilder<CommandSourceStack> =
    this.requires { it.source.isConsole() || it.player.hasPermission(permission) }

fun LiteralArgumentBuilder<CommandSourceStack>.requiresConsole(): LiteralArgumentBuilder<CommandSourceStack> =
    this.requires { it.source.isConsole() }

abstract class Command(val name: String) {
    
    var builder: LiteralArgumentBuilder<CommandSourceStack> = literal(name)
    
    fun literal(name: String): LiteralArgumentBuilder<CommandSourceStack> {
        return LiteralArgumentBuilder.literal(name)
    }
    
    fun <T> argument(name: String, argumentType: ArgumentType<T>): RequiredArgumentBuilder<CommandSourceStack, T> {
        return RequiredArgumentBuilder.argument(name, argumentType)
    }
    
}
