@file:Suppress("UnstableApiUsage")

package xyz.xenondevs.nova.command.argument

import com.mojang.brigadier.LiteralMessage
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.exceptions.CommandSyntaxException.BUILT_IN_EXCEPTIONS
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import io.papermc.paper.command.brigadier.argument.CustomArgumentType
import me.xdrop.fuzzywuzzy.FuzzySearch
import net.minecraft.resources.ResourceLocation
import org.bukkit.NamespacedKey
import java.util.concurrent.CompletableFuture

internal abstract class KeyedArgumentType<T : Any> : CustomArgumentType.Converted<T, NamespacedKey> {
    
    override fun getNativeType() = ArgumentTypes.namespacedKey()
    
    override fun convert(nativeType: NamespacedKey): T {
        val arg = if (nativeType.namespace == "minecraft")
            nativeType.key
        else nativeType.toString()
        
        val exact = getEntries()
            .firstOrNull { toId(it).toString() == arg }
        if (exact != null)
            return exact
        
        val loose = getEntries()
            .filter { toId(it).path == arg }
            .toList()
        
        when (loose.size) {
            1 -> return loose[0]
            
            0 -> throw CommandSyntaxException(
                BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument(),
                LiteralMessage("No such item: $arg")
            )
            
            else -> throw CommandSyntaxException(
                BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument(),
                LiteralMessage("Cannot decide between: $loose")
            )
        }
    }
    
    override fun <S : Any> listSuggestions(context: CommandContext<S>, builder: SuggestionsBuilder): CompletableFuture<Suggestions> {
        val arg = context.input.split(" ").last()
        val items = getEntries()
            .filter { toId(it).toString().contains(arg) }
            .toList()
        FuzzySearch.extractSorted(arg, items) { toId(it).toString() }
            .forEach { builder.suggest(toId(it.referent).toString()) }
        
        return builder.buildFuture()
    }
    
    abstract fun getEntries(): Sequence<T>
    
    abstract fun toId(t: T): ResourceLocation
    
}