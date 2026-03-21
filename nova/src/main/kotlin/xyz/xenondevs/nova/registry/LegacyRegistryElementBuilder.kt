package xyz.xenondevs.nova.registry

import net.kyori.adventure.key.Key
import net.kyori.adventure.key.Key.key
import net.minecraft.core.Registry
import net.minecraft.core.WritableRegistry
import net.minecraft.resources.Identifier
import net.minecraft.resources.RegistryOps
import net.minecraft.resources.ResourceKey
import xyz.xenondevs.nova.util.contains
import xyz.xenondevs.nova.util.register
import xyz.xenondevs.nova.util.toKey

@RegistryElementBuilderDsl
abstract class LegacyRegistryElementBuilder<T : Any> internal constructor(
    protected val registry: WritableRegistry<in T>,
    val id: Key
) {
    
    abstract fun build(): T
    
    internal open fun register(): T {
        if (id in registry)
            throw IllegalStateException("Tried to register duplicate element $id in $registry")
        
        val element = build()
        val holder = registry.register(id, element)
        holder.bindValue(element)
        return element
    }
    
}

internal fun <T : Any, B : LegacyRegistryElementBuilder<T>> buildRegistryElementLater(
    namespace: String, name: String,
    registryKey: ResourceKey<out Registry<T>>,
    makeBuilder: (Key, WritableRegistry<T>, RegistryOps.RegistryInfoLookup) -> B,
    configureBuilder: B.() -> Unit
): ResourceKey<T> {
    val id = Identifier.fromNamespaceAndPath(namespace, name)
    val key = ResourceKey.create(registryKey, id)
    registryKey.preFreeze { registry, lookup ->
        makeBuilder(id.toKey(), registry, lookup).apply(configureBuilder).register()
    }
    return key
}

@JvmName("buildRegistryElementLazily1")
internal fun <T : Any, B : LegacyRegistryElementBuilder<T>> buildRegistryElementLater(
    namespace: String, name: String,
    registryKey: ResourceKey<out Registry<T>>,
    makeBuilder: (Key, WritableRegistry<T>) -> B,
    configureBuilder: B.() -> Unit
): ResourceKey<T> {
    val id = Identifier.fromNamespaceAndPath(namespace, name)
    val key = ResourceKey.create(registryKey, id)
    registryKey.preFreeze { registry, _ ->
        makeBuilder(id.toKey(), registry).apply(configureBuilder).register()
        
    }
    return key
}

@JvmName("buildRegistryElementLazily2")
internal fun <T : Any, B : LegacyRegistryElementBuilder<T>> buildRegistryElementLater(
    namespace: String, name: String,
    registryKey: ResourceKey<out Registry<*>>,
    makeBuilder: (Key, RegistryOps.RegistryInfoLookup) -> B,
    configureBuilder: B.() -> Unit
) {
    val id = key(namespace, name)
    registryKey.preFreeze { lookup ->
        makeBuilder(id, lookup).apply(configureBuilder).register()
    }
}
