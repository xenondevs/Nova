package xyz.xenondevs.nova.command.argument

import xyz.xenondevs.nova.world.item.NovaItem
import xyz.xenondevs.nova.registry.NovaRegistries

internal object NovaItemArgumentType : KeyedArgumentType<NovaItem>() {
    override fun getEntries() = NovaRegistries.ITEM.asSequence().filter { !it.isHidden }
    override fun toId(t: NovaItem) = t.id
}