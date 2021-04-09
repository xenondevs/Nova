package xyz.xenondevs.nova.command

import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import xyz.xenondevs.nova.util.ReflectionUtils
import xyz.xenondevs.nova.util.ReflectionUtils.createPlayerFromCommandListenerWrapper

fun CommandContext<*>.getPlayer() =
    ReflectionUtils.getPlayerFromCommandListenerWrapper(source)!!

inline fun <reified V> CommandContext<*>.getArgument(name: String): V =
    getArgument(name, V::class.java)

fun <S, T : ArgumentBuilder<S, T>> ArgumentBuilder<S, T>.executesCatching(run: (CommandContext<S>) -> Unit): T {
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

abstract class PlayerCommand(val name: String, private val permission: String) {
    
    var builder: LiteralArgumentBuilder<Any> = literal(name).requires {
        val player = createPlayerFromCommandListenerWrapper(it)
        player?.hasPermission(permission) ?: false
    }
    
    fun literal(name: String): LiteralArgumentBuilder<Any> {
        return LiteralArgumentBuilder.literal(name)
    }
    
    fun <T> argument(name: String, argumentType: ArgumentType<T>): RequiredArgumentBuilder<Any, T> {
        return RequiredArgumentBuilder.argument(name, argumentType)
    }
    
}
