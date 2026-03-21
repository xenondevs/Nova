package xyz.xenondevs.nova.command.argument

import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.world.block.tileentity.network.type.NetworkType

internal object NetworkTypeArgumentType : KeyedArgumentType<NetworkType<*>>() {
    override fun getEntries() = NovaRegistries.NETWORK_TYPE.entrySet.get().asSequence()
    override fun toId(t: NetworkType<*>) = t.key
}