package xyz.xenondevs.nova.registry

import net.kyori.adventure.key.Key
import net.minecraft.core.Holder
import net.minecraft.core.Registry
import net.minecraft.core.WritableRegistry
import net.minecraft.resources.RegistryOps
import net.minecraft.resources.ResourceKey
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.mutableProvider
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.util.Key
import xyz.xenondevs.nova.util.ResourceLocation
import xyz.xenondevs.nova.util.contains
import xyz.xenondevs.nova.util.register
import xyz.xenondevs.nova.util.toKey

@DslMarker
internal annotation class RegistryElementBuilderDsl

@RegistryElementBuilderDsl
abstract class RegistryElementBuilder<T : Any> internal constructor(
    protected val registry: WritableRegistry<in T>,
    val id: Key
) {
    
    protected abstract fun build(): T
    
    internal open fun register(): T {
        if (id in registry)
            throw IllegalStateException("Tried to register duplicate element $id in $registry")
        
        val element = build()
        val holder = registry.register(id, element)
        holder.bindValue(element)
        return element
    }
    
}

@RegistryElementBuilderDsl
abstract class LazyRegistryElementBuilder<T : Any, NMS : Any> internal constructor(
    protected val registryKey: ResourceKey<Registry<NMS>>,
    private val nmsToBukkit: (Holder<NMS>) -> T,
    protected val id: Key
) {
    
    protected abstract fun build(lookup: RegistryOps.RegistryInfoLookup): NMS
    
    @Suppress("UNCHECKED_CAST")
    internal open fun register(): Provider<T> {
        val provider = mutableProvider<Holder<NMS>> {
            throw UninitializedRegistryElementException(registryKey, id)
        }
        
        registryKey.preFreeze { registry, lookup ->
            val element = build(lookup)
            val holder = registry.register(id, element)
            provider.set(holder)
        }
        
        return provider.map(nmsToBukkit)
    }
    
}

internal fun <T : Any, B : RegistryElementBuilder<T>> buildRegistryElementLater(
    addon: Addon, name: String,
    registryKey: ResourceKey<out Registry<T>>,
    makeBuilder: (Key, WritableRegistry<T>, RegistryOps.RegistryInfoLookup) -> B,
    configureBuilder: B.() -> Unit
): ResourceKey<T> {
    val id = ResourceLocation(addon, name)
    val key = ResourceKey.create(registryKey, id)
    registryKey.preFreeze { registry, lookup ->
        makeBuilder(id.toKey(), registry, lookup).apply(configureBuilder).register()
    }
    return key
}

@JvmName("buildRegistryElementLazily1")
internal fun <T : Any, B : RegistryElementBuilder<T>> buildRegistryElementLater(
    addon: Addon, name: String,
    registryKey: ResourceKey<out Registry<T>>,
    makeBuilder: (Key, WritableRegistry<T>) -> B,
    configureBuilder: B.() -> Unit
): ResourceKey<T> {
    val id = ResourceLocation(addon, name)
    val key = ResourceKey.create(registryKey, id)
    registryKey.preFreeze { registry, _ ->
        makeBuilder(id.toKey(), registry).apply(configureBuilder).register()
        
    }
    return key
}

@JvmName("buildRegistryElementLazily2")
internal fun <T : Any, B : RegistryElementBuilder<T>> buildRegistryElementLater(
    addon: Addon, name: String,
    registryKey: ResourceKey<out Registry<*>>,
    makeBuilder: (Key, RegistryOps.RegistryInfoLookup) -> B,
    configureBuilder: B.() -> Unit
) {
    val id = Key(addon, name)
    registryKey.preFreeze { lookup ->
        makeBuilder(id, lookup).apply(configureBuilder).register()
    }
}

class UninitializedRegistryElementException(registryKey: ResourceKey<*>, id: Key) : Exception(
    "Tried to access unregistered registry element $id in $registryKey"
)