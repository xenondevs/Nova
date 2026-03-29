package xyz.xenondevs.nova.registry

import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.tag.TagKey
import org.bukkit.Keyed
import xyz.xenondevs.commons.collections.mapToSet
import xyz.xenondevs.commons.provider.MutableProvider
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.mutableProvider
import xyz.xenondevs.commons.provider.provider
import java.util.concurrent.ConcurrentHashMap

@Suppress("UNCHECKED_CAST")
internal object PaperTagManager {
    
    private class TagStorage {
        val tagValues: MutableMap<TagKey<*>, MutableProvider<Set<RegistryEntry.Paper<*>>>> = ConcurrentHashMap()
        val optionalTagValues: MutableMap<TagKey<*>, MutableProvider<Provider<Set<RegistryEntry.Paper<*>>>?>> = ConcurrentHashMap()
        val allTags: MutableMap<RegistryKey<*>, MutableProvider<Set<RegistryEntrySet.Paper.Tag<*>>>> = ConcurrentHashMap()
    }
    
    private val tags = ConcurrentHashMap<RegistryAccess, TagStorage>()
    
    init {
        RegistryContext.registerPostTagReloadListener(::reloadTags)
    }
    
    private fun <T : Keyed> resolve(tagKey: TagKey<T>, registryAccess: RegistryAccess, strict: Boolean): Set<RegistryEntry.Paper<T>> {
        val registry = registryAccess.getRegistry(tagKey.registryKey())
        if (!registry.hasTag(tagKey)) {
            if (!strict)
                return emptySet()
            throw NoSuchElementException("No tag under #${tagKey.key().asString()} in registry ${tagKey.registryKey().key().asString()}")
        }
        return registry.getTagValues(tagKey).mapToSet { RegistryEntry.paper(tagKey.registryKey(), it) }
    }
    
    private fun <T : Keyed> resolveOptional(tagKey: TagKey<T>, registryAccess: RegistryAccess): Provider<Set<RegistryEntry.Paper<T>>>? {
        val registry = registryAccess.getRegistry(tagKey.registryKey())
        return if (registry.hasTag(tagKey)) {
            getTagEntries(tagKey, registryAccess)
        } else null
    }
    
    private fun <T : Keyed> resolveAllTags(registryKey: RegistryKey<T>, registryAccess: RegistryAccess): Set<RegistryEntrySet.Paper.Tag<T>> {
        val registry = registryAccess.getRegistry(registryKey)
        return registry.tags.mapToSet { registryEntrySetOf(it.tagKey(), registryAccess) }
    }
    
    private fun reloadTags() {
        for ((registryAccess, tagStorage) in tags) {
            for ((tagKey, provider) in tagStorage.tagValues) {
                tagKey as TagKey<Keyed>
                provider.set(resolve(tagKey, registryAccess, false))
            }
            
            for ((tagKey, provider) in tagStorage.optionalTagValues) {
                tagKey as TagKey<Keyed>
                provider.set(resolveOptional(tagKey, registryAccess))
            }
            
            for ((registryKey, provider) in tagStorage.allTags) {
                registryKey as RegistryKey<Keyed>
                provider.set(resolveAllTags(registryKey, registryAccess))
            }
        }
    }
    
    fun <T : Keyed> getTagEntries(tagKey: TagKey<T>, registryAccess: RegistryAccess): Provider<Set<RegistryEntry.Paper<T>>> {
        val storage = tags.computeIfAbsent(registryAccess) { TagStorage() }
        return storage.tagValues.computeIfAbsent(tagKey) {
            if (RegistryContext.isInBootstrapPhase) {
                val provider = mutableProvider<Set<RegistryEntry.Paper<*>>> { resolve(tagKey, registryAccess, true) }
                RegistryContext.trackUnresolvedTag(tagKey, registryAccess)
                provider
            } else mutableProvider(resolve(tagKey, registryAccess, true))
        } as Provider<Set<RegistryEntry.Paper<T>>>
    }
    
    fun <T : Keyed> getOptionalTagEntries(tagKey: TagKey<T>, registryAccess: RegistryAccess): Provider<Provider<Set<RegistryEntry.Paper<T>>>?> {
        val storage = tags.computeIfAbsent(registryAccess) { TagStorage() }
        return storage.optionalTagValues.computeIfAbsent(tagKey) {
            mutableProvider { resolveOptional(tagKey, registryAccess) }
        } as Provider<Provider<Set<RegistryEntry.Paper<T>>>?>
    }
    
    fun <T : Keyed> getAllEntries(registryKey: RegistryKey<T>, registryAccess: RegistryAccess): Provider<Set<RegistryEntry.Paper<T>>> {
        return provider {
            val registry = registryAccess.getRegistry(registryKey)
            registry.mapToSet { RegistryEntry.paper(registryKey, it) }
        }
    }
    
    fun <T : Keyed> getAllTags(
        registryKey: RegistryKey<T>,
        registryAccess: RegistryAccess = RegistryAccess.registryAccess()
    ): Provider<Set<RegistryEntrySet.Paper.Tag<T>>> {
        val storage = tags.computeIfAbsent(registryAccess) { TagStorage() }
        return storage.allTags.computeIfAbsent(registryKey) {
            mutableProvider { resolveAllTags(registryKey, registryAccess) }
        } as Provider<Set<RegistryEntrySet.Paper.Tag<T>>>
    }
    
}
