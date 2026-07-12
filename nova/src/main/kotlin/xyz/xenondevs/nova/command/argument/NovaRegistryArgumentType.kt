package xyz.xenondevs.nova.command.argument

import net.kyori.adventure.key.Key
import xyz.xenondevs.nova.registry.NovaRegistry
import xyz.xenondevs.nova.registry.NovaRegistryElement

internal class NovaRegistryArgumentType<T : NovaRegistryElement<T>>(
    val registry: NovaRegistry<T>
) : KeyedArgumentType<T>() {
    override fun getEntries() = registry.entrySet.get().asSequence()
    override fun toId(t: T): Key = t.entry.key
}