package xyz.xenondevs.nova.registry

import io.papermc.paper.tag.TagEventConfig
import net.kyori.adventure.key.Key
import net.minecraft.core.Registry
import net.minecraft.core.WritableRegistry
import net.minecraft.resources.Identifier
import net.minecraft.resources.RegistryOps
import net.minecraft.resources.ResourceKey
import net.minecraft.tags.TagEntry
import net.minecraft.tags.TagKey
import net.minecraft.tags.TagLoader
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.config.MAIN_CONFIG
import xyz.xenondevs.nova.config.entry
import xyz.xenondevs.nova.util.set
import xyz.xenondevs.nova.util.toIdentifier

private val LOG_REGISTRY_FREEZE by MAIN_CONFIG.entry<Boolean>("debug", "logging", "registry_freeze")

private typealias PreFreezeListener<T> = (registry: WritableRegistry<T>, lookup: RegistryOps.RegistryInfoLookup) -> Unit
private typealias PostFreezeListener<T> = (registry: Registry<T>, lookup: RegistryOps.RegistryInfoLookup) -> Unit

internal object RegistryEventManager {
    
    private val preFreezeListeners = HashMap<ResourceKey<*>, ArrayList<PreFreezeListener<*>>>()
    private val postFreezeListeners = HashMap<ResourceKey<*>, ArrayList<PostFreezeListener<*>>>()
    private val frozen = HashSet<ResourceKey<*>>()
    private val additionalTagEntries = HashMap<ResourceKey<*>, HashMap<TagKey<*>, ArrayList<Identifier>>>()
    
    @JvmStatic
    fun handlePreFreeze(registry: WritableRegistry<*>, lookup: RegistryOps.RegistryInfoLookup) {
        val key = registry.key()
        try {
            preFreezeListeners[key]?.forEach { it(registry, lookup) }
            preFreezeListeners.remove(key)
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
            postFreezeListeners[key]?.forEach { it(registry, lookup) }
            postFreezeListeners.remove(key)
        } catch (t: Throwable) {
            LOGGER.error("An exception occurred while running registry post-freeze listeners for $key", t)
        }
    }
    
    @JvmStatic
    fun handleTagsBuild(
        map: MutableMap<Identifier, MutableList<TagLoader.EntryWithSource>>, // Map<Tag ID, List<Entry ID / other Tag ID>>
        config: TagEventConfig<*, *>?
    ) {
        if (config == null)
            return
        
        val key = ResourceKey.createRegistryKey<Any>(config.apiRegistryKey().key().toIdentifier())
        val additionalEntriesForRegistry = additionalTagEntries[key]
            ?: return
        
        for ((tagKey, entries) in additionalEntriesForRegistry) {
            val mappedEntries = entries.map { TagLoader.EntryWithSource(TagEntry.element(it), "Nova") }
            map.getOrPut(tagKey.location, ::ArrayList) += mappedEntries
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
    
    fun addTagEntry(key: TagKey<*>, value: Identifier) {
        additionalTagEntries
            .getOrPut(key.registry(), ::HashMap)
            .getOrPut(key, ::ArrayList) += value
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

internal operator fun TagKey<*>.plusAssign(id: Identifier) {
    RegistryEventManager.addTagEntry(this, id)
}

internal operator fun TagKey<*>.plusAssign(id: Key) {
    this += id.toIdentifier()
}