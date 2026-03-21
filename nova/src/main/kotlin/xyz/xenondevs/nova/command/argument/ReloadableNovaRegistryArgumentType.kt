package xyz.xenondevs.nova.command.argument

import xyz.xenondevs.nova.registry.MutableNovaRegistry
import xyz.xenondevs.nova.registry.NovaRegistries

internal object ReloadableNovaRegistryArgumentType : KeyedArgumentType<MutableNovaRegistry<*>>() {
    override fun getEntries() = NovaRegistries.registries.values.asSequence().filter { it.isReloadable }
    override fun toId(t: MutableNovaRegistry<*>) = t.key
}