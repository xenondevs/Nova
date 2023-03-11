@file:Suppress("DeprecatedCallableAddReplaceWith")

package xyz.xenondevs.nova.command

import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import net.kyori.adventure.text.Component
import net.md_5.bungee.api.chat.BaseComponent
import net.minecraft.commands.CommandSource
import net.minecraft.commands.CommandSourceStack
import net.minecraft.server.dedicated.DedicatedServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.rcon.RconConsoleSource
import org.bukkit.entity.Player
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.util.component.adventure.toNMSComponent
import xyz.xenondevs.nova.util.component.bungee.toComponent
import java.util.logging.Level

val CommandContext<CommandSourceStack>.player: Player
    get() = source.bukkitPlayer

val CommandSourceStack.bukkitPlayer: Player
    get() = playerOrException.bukkitEntity

fun CommandSourceStack.sendSuccess(message: Component, broadcast: Boolean = false) {
    sendSuccess(message.toNMSComponent(), broadcast)
}

fun CommandSourceStack.sendFailure(message: Component) {
    sendFailure(message.toNMSComponent())
}

fun CommandSource.isConsole() = this is DedicatedServer || this is RconConsoleSource

fun CommandSource.isPlayer() = this is ServerPlayer

inline operator fun <reified V> CommandContext<*>.get(name: String): V =
    getArgument(name, V::class.java)

fun <CommandSourceStack, T : ArgumentBuilder<CommandSourceStack, T>> ArgumentBuilder<CommandSourceStack, T>.executesCatching(run: (CommandContext<CommandSourceStack>) -> Unit): T {
    return executes {
        try {
            run(it)
        } catch (e: Exception) {
            LOGGER.log(Level.SEVERE, "An exception occurred while running a command", e)
        }
        
        0
    }
}

fun LiteralArgumentBuilder<CommandSourceStack>.requiresPlayerPermission(permission: String): LiteralArgumentBuilder<CommandSourceStack> =
    this.requires { it.source.isPlayer() && it.bukkitPlayer.hasPermission(permission) }

fun LiteralArgumentBuilder<CommandSourceStack>.requiresPermission(permission: String): LiteralArgumentBuilder<CommandSourceStack> =
    this.requires { !it.source.isPlayer() || it.bukkitPlayer.hasPermission(permission) }

fun LiteralArgumentBuilder<CommandSourceStack>.requiresConsole(): LiteralArgumentBuilder<CommandSourceStack> =
    this.requires { it.source.isConsole() }

fun LiteralArgumentBuilder<CommandSourceStack>.requiresPlayer(): LiteralArgumentBuilder<CommandSourceStack> =
    this.requires { it.source.isPlayer() }

//<editor-fold desc="deprecated">
@Deprecated("Use adventure components instead")
fun CommandSourceStack.sendSuccess(message: Array<out BaseComponent>) {
    sendSuccess(message.toComponent(), false)
}

@Deprecated("Use adventure components instead")
@JvmName("sendSuccess1")
fun CommandSourceStack.sendSuccess(vararg message: BaseComponent) {
    this.sendSuccess(message.toComponent(), false)
}

@Deprecated("Use adventure components instead")
fun CommandSourceStack.sendFailure(message: Array<out BaseComponent>) {
    sendFailure(message.toComponent())
}

@Deprecated("Use adventure components instead")
@JvmName("sendFailure1")
fun CommandSourceStack.sendFailure(vararg message: BaseComponent) {
    sendFailure(message.toComponent())
}
//</editor-fold>

abstract class Command(val name: String) {
    
    var builder: LiteralArgumentBuilder<CommandSourceStack> = literal(name)
    
    fun literal(name: String): LiteralArgumentBuilder<CommandSourceStack> {
        return LiteralArgumentBuilder.literal(name)
    }
    
    fun <T> argument(name: String, argumentType: ArgumentType<T>): RequiredArgumentBuilder<CommandSourceStack, T> {
        return RequiredArgumentBuilder.argument(name, argumentType)
    }
    
}
