package xyz.xenondevs.nova.command.argument

import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.SuggestionProvider
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import io.papermc.paper.command.brigadier.CommandSourceStack
import xyz.xenondevs.nova.util.data.UpdatableFile
import java.util.concurrent.CompletableFuture

internal object UpdatableFileSuggestionProvider : SuggestionProvider<CommandSourceStack> {
    
    override fun getSuggestions(context: CommandContext<CommandSourceStack>, builder: SuggestionsBuilder): CompletableFuture<Suggestions> {
        val paths = UpdatableFile.getTrackedFilePaths()
        val starPaths = HashSet<String>()
        starPaths += "*"
        for (path in paths) {
            var i = path.indexOf('/')
            while (i != -1) {
                starPaths += path.substring(0, i + 1) + "*"
                i = path.indexOf('/', i + 1)
            }
        }
        
        for (path in paths + starPaths) {
            if (path.startsWith(builder.remaining)) {
                builder.suggest(path)
            }
        }
        
        return builder.buildFuture()
    }
    
}