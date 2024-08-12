package xyz.xenondevs.nova.registry

import com.mojang.serialization.Lifecycle
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import net.minecraft.core.Registry
import net.minecraft.core.RegistryAccess
import net.minecraft.core.WritableRegistry
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.util.ResourceLocation
import java.util.*
import java.util.stream.Stream
import kotlin.streams.asStream

@Suppress("UNCHECKED_CAST")
object NovaRegistryAccess : RegistryAccess {
    
    private val registries = Object2ObjectOpenHashMap<ResourceKey<out Registry<*>>, Registry<*>>()
    
    override fun <E : Any> registry(registry: ResourceKey<out Registry<out E>>): Optional<Registry<E>> {
        return Optional.ofNullable(registries[registry] as Registry<E>)
    }
    
    override fun registries(): Stream<RegistryAccess.RegistryEntry<*>> = registries.asSequence().map {
        RegistryAccess.RegistryEntry(it.key as ResourceKey<out Registry<Any>>, it.value as Registry<Any>)
    }.asStream()
    
    fun <E : Any> addRegistry(
        addon: Addon,
        registryName: String
    ): WritableRegistry<E> = addRegistry(ResourceLocation(addon, registryName))
    
    fun <E : Any> addFuzzyRegistry(
        addon: Addon,
        registryName: String
    ): FuzzyMappedRegistry<E> = addRegistry(ResourceLocation(addon, registryName), ::FuzzyMappedRegistry)
    
    fun <E : Any, R : Registry<E>> addRegistry(
        addon: Addon,
        registryName: String,
        registryConstructor: (ResourceKey<out R>, Lifecycle) -> R
    ): R = addRegistry(ResourceLocation(addon, registryName), registryConstructor)
    
    internal fun <E : Any, R : Registry<E>> addRegistry(
        registryName: ResourceLocation,
        registryConstructor: (ResourceKey<out R>, Lifecycle) -> R
    ): R {
        val resourceKey = ResourceKey.createRegistryKey<E>(registryName)
        require(resourceKey !in registries) { "Duplicate registry $resourceKey" }
        val registry = registryConstructor(resourceKey as ResourceKey<out R>, Lifecycle.stable())
        require(registry.key() == resourceKey) { "Registry $resourceKey has wrong key (expected $resourceKey, got ${registry.key()})" }
        registries[resourceKey] = registry
        return registry
    }
    
    internal fun <E : Any> addRegistry(
        namespace: String,
        path: String
    ): WritableRegistry<E> = addRegistry(ResourceLocation.fromNamespaceAndPath(namespace, path), ::InstantBindMappedRegistry)
    
    internal fun <E : Any> addRegistry(
        registryName: ResourceLocation
    ): WritableRegistry<E> = addRegistry(registryName, ::InstantBindMappedRegistry)
    
    internal fun <E : Any> addFuzzyRegistry(
        registryName: ResourceLocation
    ): FuzzyMappedRegistry<E> = addRegistry(registryName, ::FuzzyMappedRegistry)
    
    
    internal fun freezeAll() {
        registries.forEach { it.value.freeze() }
    }
    
}