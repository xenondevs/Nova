package xyz.xenondevs.nova.command.argument

import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.world.block.NovaBlock

internal object NovaBlockArgumentType : KeyedArgumentType<NovaBlock>() {
    override fun getEntries() = NovaRegistries.BLOCK.asSequence()
    override fun toId(t: NovaBlock) = t.id
}