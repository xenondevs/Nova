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
    }
    
    private val tags = ConcurrentHashMap<RegistryAccess, TagStorage>()
    
    init {
        RegistryContext.registerPostTagReloadListener(::reloadTags)
    }
    
    private fun <T : Keyed> resolve(tagKey: TagKey<T>, registryAccess: RegistryAccess): Set<RegistryEntry.Paper<T>> {
        val registry = registryAccess.getRegistry(tagKey.registryKey())
        if (!registry.hasTag(tagKey))
            throw NoSuchElementException("No tag under #${tagKey.key().asString()} in registry ${tagKey.registryKey().key().asString()}")
        return registry.getTagValues(tagKey).mapToSet { RegistryEntry.paper(tagKey.registryKey(), it) }
    }
    
    private fun <T : Keyed> resolveOptional(tagKey: TagKey<T>, registryAccess: RegistryAccess): Provider<Set<RegistryEntry.Paper<T>>>? {
        val registry = registryAccess.getRegistry(tagKey.registryKey())
        return if (registry.hasTag(tagKey)) {
            getTagEntries(tagKey, registryAccess)
        } else null
    }
    
    private fun reloadTags() {
        for ((registryAccess, tagStorage) in tags) {
            for ((tagKey, provider) in tagStorage.tagValues) {
                tagKey as TagKey<Keyed>
                provider.set(resolve(tagKey, registryAccess))
            }
            
            for ((tagKey, provider) in tagStorage.optionalTagValues) {
                tagKey as TagKey<Keyed>
                provider.set(resolveOptional(tagKey, registryAccess))
            }
        }
    }
    
    fun <T : Keyed> getTagEntries(tagKey: TagKey<T>, registryAccess: RegistryAccess): Provider<Set<RegistryEntry.Paper<T>>> {
        val storage = tags.computeIfAbsent(registryAccess) { TagStorage() }
        return storage.tagValues.computeIfAbsent(tagKey) {
            if (RegistryContext.isInBootstrapPhase) {
                val provider = mutableProvider<Set<RegistryEntry.Paper<*>>> { resolve(tagKey, registryAccess) }
                RegistryContext.trackUnresolved(tagKey, provider)
                provider
            } else mutableProvider(resolve(tagKey, registryAccess))
        } as Provider<Set<RegistryEntry.Paper<T>>>
    }
    
    fun <T : Keyed> getOptionalTagEntries(tagKey: TagKey<T>, registryAccess: RegistryAccess): Provider<Provider<Set<RegistryEntry.Paper<T>>>?> {
        val storage = tags.computeIfAbsent(registryAccess) { TagStorage() }
        return storage.optionalTagValues.computeIfAbsent(tagKey) {
            mutableProvider { resolveOptional(tagKey, registryAccess) }
        } as Provider<Provider<Set<RegistryEntry.Paper<T>>>?>
    }
    
    fun <T : Keyed> getAllEntries(registryKey: RegistryKey<T>, registryAccess: RegistryAccess): Provider<Set<RegistryEntry.Paper<T>>> {
        fun resolve(): Set<RegistryEntry.Paper<T>> {
            val registry = registryAccess.getRegistry(registryKey)
            return registry.mapToSet { RegistryEntry.paper(registryKey, it) }
        }
        
        return if (RegistryContext.isInBootstrapPhase)
            provider(::resolve)
        else provider(resolve())
    }
    
}
