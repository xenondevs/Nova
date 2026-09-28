package xyz.xenondevs.nova.command.argument

import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.SuggestionProvider
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import io.papermc.paper.command.brigadier.CommandSourceStack
import xyz.xenondevs.nova.registry.KnownRegistryEntries
import java.util.concurrent.CompletableFuture

internal object KnownElementSuggestionProvider : SuggestionProvider<CommandSourceStack> {
    
    override fun getSuggestions(context: CommandContext<CommandSourceStack>, builder: SuggestionsBuilder): CompletableFuture<Suggestions> {
        fun maybeSuggest(s: String) {
            if (s.startsWith(builder.remaining))
                builder.suggest(s)
        }
        
        maybeSuggest("*")
        for ([registryKey, valueKeys] in KnownRegistryEntries.knownRegistryEntries) {
            maybeSuggest("${registryKey.asString()}/*")
            for (valueKey in valueKeys) {
                maybeSuggest("${registryKey.asString()}/${valueKey.asString()}")
            }
        }
        
        return builder.buildFuture()
    }
    
}

