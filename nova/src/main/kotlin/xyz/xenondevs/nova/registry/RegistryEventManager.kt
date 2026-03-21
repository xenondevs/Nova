package xyz.xenondevs.nova.registry

import net.kyori.adventure.key.Key
import net.minecraft.core.Registry
import net.minecraft.core.RegistryAccess
import net.minecraft.core.WritableRegistry
import net.minecraft.resources.Identifier
import net.minecraft.resources.RegistryOps
import net.minecraft.resources.ResourceKey
import xyz.xenondevs.commons.collections.concurrentHashSet
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.config.MAIN_CONFIG
import xyz.xenondevs.nova.config.entry
import xyz.xenondevs.nova.util.set
import xyz.xenondevs.nova.util.toIdentifier
import java.util.concurrent.ConcurrentHashMap

private val LOG_REGISTRY_FREEZE by MAIN_CONFIG.entry<Boolean>("debug", "logging", "registry_freeze")

private typealias PreFreezeListener<T> = (registry: WritableRegistry<T>, lookup: RegistryOps.RegistryInfoLookup) -> Unit
private typealias PostFreezeListener<T> = (registry: Registry<T>, lookup: RegistryOps.RegistryInfoLookup) -> Unit

internal object RegistryEventManager {
    
    private val preFreezeListeners = ConcurrentHashMap<ResourceKey<*>, ArrayList<PreFreezeListener<*>>>()
    private val postFreezeListeners = ConcurrentHashMap<ResourceKey<*>, ArrayList<PostFreezeListener<*>>>()
    private val frozen = concurrentHashSet<ResourceKey<*>>()
    
    @JvmStatic
    fun handlePreFreeze(registry: WritableRegistry<*>, lookup: RegistryOps.RegistryInfoLookup) {
        val key = registry.key()
        try {
            preFreezeListeners.remove(key)?.forEach { it(registry, lookup) }
        } catch (t: Throwable) {
            LOGGER.error("An exception occurred while running registry pre-freeze listeners for $key", t)
        }
        
        if (LOG_REGISTRY_FREEZE) {
            LOGGER.info("Freezing registry $key")
        }
    }
    
    @JvmStatic
    fun handlePostFreeze(registry: Registry<*>, lookup: RegistryOps.RegistryInfoLookup) {
        val key = registry.key()
        try {
            postFreezeListeners.remove(key)?.forEach { it(registry, lookup) }
        } catch (t: Throwable) {
            LOGGER.error("An exception occurred while running registry post-freeze listeners for $key", t)
        }
    }
    
    @JvmStatic
    fun handlePostFreeze(registryAccess: RegistryAccess) {
        val lookup = RegistryOps.HolderLookupAdapter(registryAccess)
        for (registryEntry in registryAccess.registries()) {
            val key = registryEntry.key
            val registry = registryEntry.value
            
            try {
                postFreezeListeners.remove(key)?.forEach { it(registry, lookup) }
            } catch (t: Throwable) {
                LOGGER.error("An exception occurred while running registry post-freeze listeners for $key", t)
            }
        }
    }
    
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> addPreFreezeListener(key: ResourceKey<out Registry<T>>, listener: PreFreezeListener<T>) {
        check(key !in frozen) { "Registry $key is already frozen!" }
        preFreezeListeners.getOrPut(key, ::ArrayList) += listener as PreFreezeListener<*>
    }
    
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> addPostFreezeListener(key: ResourceKey<out Registry<T>>, listener: PostFreezeListener<T>) {
        check(key !in frozen) { "Registry $key is already frozen!" }
        postFreezeListeners.getOrPut(key, ::ArrayList) += listener as PostFreezeListener<*>
    }
    
}

internal fun <T : Any> ResourceKey<out Registry<T>>.preFreeze(listener: PreFreezeListener<T>) {
    RegistryEventManager.addPreFreezeListener(this, listener)
}

internal fun <T : Any> ResourceKey<out Registry<T>>.postFreeze(listener: PostFreezeListener<T>) {
    RegistryEventManager.addPostFreezeListener(this, listener)
}

// kotlin seems to not be able to resolve the above function if there's no T, even if registry is unused
@Suppress("UNCHECKED_CAST")
internal fun ResourceKey<out Registry<*>>.preFreeze(listener: (lookup: RegistryOps.RegistryInfoLookup) -> Unit) {
    (this as ResourceKey<Registry<Any>>).preFreeze { _, lookup -> listener(lookup) }
}

internal operator fun <T : Any> ResourceKey<out Registry<T>>.set(id: Identifier, value: T) {
    preFreeze { registry, _ -> registry[id] = value }
}

internal operator fun <T : Any> ResourceKey<out Registry<T>>.set(id: Key, value: T) {
    this[id.toIdentifier()] = value
}

internal operator fun <T : Any> ResourceKey<out Registry<T>>.set(id: ResourceKey<T>, value: T) {
    preFreeze { registry, _ -> registry[id] = value }
}