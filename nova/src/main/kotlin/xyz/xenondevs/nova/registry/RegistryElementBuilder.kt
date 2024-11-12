package xyz.xenondevs.nova.registry

import net.minecraft.core.Registry
import net.minecraft.core.WritableRegistry
import net.minecraft.resources.RegistryOps
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.mutableProvider
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.patch.impl.registry.preFreeze
import xyz.xenondevs.nova.util.ResourceLocation
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.HOLDER_REFERENCE_BIND_VALUE_METHOD
import xyz.xenondevs.nova.util.register
import xyz.xenondevs.nova.util.set

@DslMarker
internal annotation class RegistryElementBuilderDsl

@RegistryElementBuilderDsl
abstract class RegistryElementBuilder<T : Any>(
    protected val registry: WritableRegistry<in T>,
    val id: ResourceLocation
) {
    
    protected abstract fun build(): T
    
    internal open fun register(): T {
        if (registry.containsKey(id))
            throw IllegalStateException("Tried to register duplicate element $id in $registry")
        
        val element = build()
        val holder = registry.register(id, element)
        HOLDER_REFERENCE_BIND_VALUE_METHOD.invoke(holder, element)
        return element
    }
    
}

abstract class LazyRegistryElementBuilder<T : Any>(
    protected val registryKey: ResourceKey<Registry<T>>,
    protected val id: ResourceLocation
) {
    
    protected abstract fun build(): T
    
    @Suppress("UNCHECKED_CAST")
    internal open fun register(): Provider<T> {
        val provider = mutableProvider<T> {
            throw UninitializedRegistryElementException(registryKey, id) 
        }
        
        registryKey.preFreeze { registry, _ ->
            if (registry.containsKey(id))
                throw IllegalStateException("Tried to register duplicate element $id in $registryKey")
            
            val element = build()
            registry[id] = element
            provider.set(element)
        }
        
        return provider
    }
    
}

internal fun <T : Any, B : RegistryElementBuilder<T>> buildRegistryElementLater(
    addon: Addon, name: String,
    registryKey: ResourceKey<out Registry<T>>, 
    makeBuilder: (ResourceLocation, WritableRegistry<T>, RegistryOps.RegistryInfoLookup) -> B,
    configureBuilder: B.() -> Unit
): ResourceKey<T> {
    val id = ResourceLocation(addon, name)
    val key = ResourceKey.create(registryKey, id)
    registryKey.preFreeze { registry, lookup ->
        makeBuilder(id, registry, lookup).apply(configureBuilder).register()
    }
    return key
}

@JvmName("buildRegistryElementLazily1")
internal fun <T : Any, B : RegistryElementBuilder<T>> buildRegistryElementLater(
    addon: Addon, name: String,
    registryKey: ResourceKey<out Registry<T>>,
    makeBuilder: (ResourceLocation, WritableRegistry<T>) -> B,
    configureBuilder: B.() -> Unit
): ResourceKey<T> {
    val id = ResourceLocation(addon, name)
    val key = ResourceKey.create(registryKey, id)
    registryKey.preFreeze { registry, _ ->
        makeBuilder(id, registry).apply(configureBuilder).register()
        
    }
    return key
}

@JvmName("buildRegistryElementLazily2")
internal fun <T : Any, B : RegistryElementBuilder<T>> buildRegistryElementLater(
    addon: Addon, name: String,
    registryKey: ResourceKey<out Registry<*>>,
    makeBuilder: (ResourceLocation, RegistryOps.RegistryInfoLookup) -> B,
    configureBuilder: B.() -> Unit
) {
    val id = ResourceLocation(addon, name)
    registryKey.preFreeze { lookup ->
        makeBuilder(id, lookup).apply(configureBuilder).register()
    }
}

class UninitializedRegistryElementException(registryKey: ResourceKey<*>, id: ResourceLocation) : Exception(
    "Tried to access unregistered registry element $id in $registryKey"
)