package xyz.xenondevs.nova.command.argument

import net.kyori.adventure.key.Key
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder

internal object ResourcePackIdArgumentType : KeyedArgumentType<Key>() {
    
    override fun getEntries(): Sequence<Key> =
        ResourcePackBuilder.configurations.keys.asSequence()
    
    override fun toId(t: Key): Key = t
    
}