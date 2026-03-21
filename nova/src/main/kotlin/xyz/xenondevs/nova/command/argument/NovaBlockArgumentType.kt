package xyz.xenondevs.nova.command.argument

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.level.block.Block
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.util.toKey
import xyz.xenondevs.nova.world.block.NovaBlock

internal object NovaBlockArgumentType : KeyedArgumentType<NovaBlock>() {
    override fun getEntries() = NovaRegistries.BLOCK.entrySet.get().asSequence()
    override fun toId(t: NovaBlock) = t.key
}

internal object VanillaBlockArgumentType : KeyedArgumentType<Block>() {
    override fun getEntries() = BuiltInRegistries.BLOCK.asSequence()
    override fun toId(t: Block) = BuiltInRegistries.BLOCK.getKey(t).toKey()
}