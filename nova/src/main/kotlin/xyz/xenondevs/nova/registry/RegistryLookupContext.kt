package xyz.xenondevs.nova.registry

import net.minecraft.core.Holder
import net.minecraft.core.HolderGetter
import net.minecraft.core.Registry
import net.minecraft.resources.RegistryOps
import net.minecraft.resources.ResourceKey
import xyz.xenondevs.nova.util.lookupGetterOrThrow

/**
 * Gets the [HolderGetter] for the registry key through [ctx].
 */
context(ctx: RegistryLookupContext)
fun <T : Any> ResourceKey<Registry<T>>.holderGetter(): HolderGetter<T> =
    ctx.lookup.lookupGetterOrThrow(this)

/**
 * Gets the [Holder] for the registry key through [ctx].
 */
context(ctx: RegistryLookupContext)
fun <T : Any> ResourceKey<T>.holder(): Holder<T> =
    registryKey().holderGetter().getOrThrow(this)

/**
 * Provides access to [RegistryOps.RegistryInfoLookup].
 */
sealed interface RegistryLookupContext {
    val lookup: RegistryOps.RegistryInfoLookup
}

internal class RegistryLookupContextImpl(
    override val lookup: RegistryOps.RegistryInfoLookup
) : RegistryLookupContext
