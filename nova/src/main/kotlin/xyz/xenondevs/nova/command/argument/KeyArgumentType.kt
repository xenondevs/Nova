package xyz.xenondevs.nova.command.argument

import net.kyori.adventure.key.Key

internal class KeyArgumentType(private val values: Iterable<Key>) : KeyedArgumentType<Key>() {
    override fun getEntries() = values.asSequence()
    override fun toId(t: Key) = t
}