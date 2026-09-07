package xyz.xenondevs.nova.registry

import net.kyori.adventure.key.Key
import xyz.xenondevs.commons.collections.mapToSet
import xyz.xenondevs.commons.provider.DeferredValue
import xyz.xenondevs.commons.provider.MutableProvider
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.combinedProvider
import xyz.xenondevs.commons.provider.flatten
import xyz.xenondevs.commons.provider.mutableProvider
import xyz.xenondevs.commons.provider.provider
import xyz.xenondevs.commons.provider.uninitializedProvider
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal abstract class AbstractNovaRegistry<T : NovaRegistryElement<T>>(
    override val key: Key,
    protected val unknownEntryFactory: ((RegistryEntry.Nova<T>) -> T)?
) : MutableNovaRegistry<T> {
    
    final override val unmodifiableView: NovaRegistry<T>
        get() = UnmodifiableNovaRegistry(this)
    
    protected val lock = ReentrantLock()
    
    // storage: store raw values, cleared and repopulated on reload
    protected val entryByKey: MutableMap<Key, T> = LinkedHashMap()
    protected val keyByEntry: MutableMap<T, Key> = HashMap()
    protected val entriesByName: MutableMap<String, MutableList<T>> = HashMap()
    protected val unflattenedTagEntriesByKey: MutableMap<Key, Provider<Set<NovaTagEntry<T>>>> = HashMap()
    protected val missingEntryKeys: MutableSet<Key> = LinkedHashSet()
    
    // providers: updated on reload, maps are greedy
    protected val entryProviders: MutableMap<Key, Provider<T>> = HashMap()
    protected val optionalEntryProviders: MutableMap<Key, Provider<RegistryEntry.Nova<T>?>> = HashMap()
    protected val tagProvidersHolder: MutableProvider<Provider<Map<Key, Set<RegistryEntry.Nova<T>>>>> = uninitializedProvider()
    protected val tagProviders: Provider<Map<Key, Set<RegistryEntry.Nova<T>>>> = tagProvidersHolder.flatten()
    protected val optionalTagProviders: MutableMap<Key, Provider<RegistryEntrySet.Nova.Tag<T>?>> = HashMap()
    
    // caches: cached objects for types that wrap providers, updated implicitly on reload, maps are greedy
    protected val entries: MutableMap<Key, RegistryEntry.Nova<T>> = HashMap()
    protected val tagsByKey: MutableMap<Key, RegistryEntrySet.Nova.Tag<T>> = HashMap()
    
    override val tags: Provider<Set<RegistryEntrySet.Nova.Tag<T>>> = createProvider { tagsByKey.values.toSet() }
    
    protected var isFrozen = false
    
    final override operator fun set(key: Key, value: T): Unit = lock.withLock {
        checkNotFrozen()
        require(key !in entryByKey) { "$key is already registered" }
        require(value !in keyByEntry) { "$value is already registered with key ${keyByEntry[value]}" }
        
        entryByKey[key] = value
        keyByEntry[value] = key
        entriesByName.getOrPut(key.value()) { mutableListOf() } += value
        missingEntryKeys -= key
    }
    
    final override fun setKnown(key: Key): Unit = lock.withLock {
        checkNotFrozen()
        if (unknownEntryFactory != null)
            missingEntryKeys += key
    }
    
    final override fun set(tagKey: Key, entries: Provider<Set<NovaTagEntry<T>>>): Unit = lock.withLock {
        checkNotFrozen()
        require(tagKey != key) { "Tag key cannot match registry key, as that is reserved for the entrySet tag" }
        require(tagKey !in unflattenedTagEntriesByKey) { "Tag $tagKey is already registered" }
        
        unflattenedTagEntriesByKey[tagKey] = entries
    }
    
    final override fun getValue(key: Key): T? = lock.withLock {
        checkFrozen()
        if (key !in this)
            return null
        return entryByKey[key]
    }
    
    final override fun getValuesByName(name: String): List<T> = lock.withLock {
        checkFrozen()
        return entriesByName[name]?.toList() ?: emptyList()
    }
    
    final override fun contains(key: Key): Boolean = lock.withLock {
        checkFrozen()
        return key in entryByKey
    }
    
    final override fun contains(value: T): Boolean = lock.withLock {
        checkFrozen()
        return value in keyByEntry
    }
    
    final override fun get(key: Key): RegistryEntry.Nova<T> = lock.withLock {
        if (isFrozen) {
            require(key in entryByKey) { "Cannot create entry for unregistered key $key after freezing" }
            return entries[key]!!
        } else {
            return entries.getOrPut(key) {
                val provider = entryProviders.getOrPutEagerProvider(key) { getValueOrThrow(key) }
                NovaRegistryEntry(this, key, provider)
            }
        }
    }
    
    final override fun getOptional(key: Key): Provider<RegistryEntry.Nova<T>?> = lock.withLock {
        return optionalEntryProviders.getOrPutLazyProvider(key) { entries[key] }
    }
    
    final override fun getTag(key: Key): RegistryEntrySet.Nova.Tag<T> = lock.withLock {
        if (isFrozen) {
            require(key in tagsByKey) { "Cannot create tag entry set for unregistered tag key $key after freezing" }
            return tagsByKey[key]!!
        } else {
            return tagsByKey.getOrPut(key) {
                val entries = tagProviders.map { tags ->
                    tags[key] ?: throw NoSuchElementException("No tag found for key $key")
                }
                NovaTagRegistryEntrySet(this, key, entries)
            }
        }
    }
    
    final override fun getOptionalTag(key: Key): Provider<RegistryEntrySet.Nova.Tag<T>?> = lock.withLock {
        return optionalTagProviders.getOrPutLazyProvider(key) { tagsByKey[key] }
    }
    
    final override fun freeze() {
        val flattenedTagContentsProvider = lock.withLock { freezeLocked() }
        tagProvidersHolder.set(flattenedTagContentsProvider)
    }
    
    protected fun freezeLocked(): Provider<Map<Key, Set<RegistryEntry.Nova<T>>>> {
        check(lock.isHeldByCurrentThread)
        checkNotFrozen()
        
        registerUnknownEntries()
        
        // populate entries map & register "all entries" tag
        unflattenedTagEntriesByKey[key] = provider(entryByKey.keys.mapToSet { NovaTagEntry.Direct(get(it)) })
        // populate tags map
        unflattenedTagEntriesByKey.keys.forEach { getTag(it) }
        
        isFrozen = true
        
        return try {
            val flattenedTagContentsProvider = buildFlattenedTagContentsProvider()
            
            // bind entry providers and validate the initial flattened tag contents
            entries.values.forEach { it.get() }
            flattenedTagContentsProvider.get()
            
            flattenedTagContentsProvider
        } catch (e: NoSuchElementException) {
            isFrozen = false // freezing failed
            throw IllegalStateException("Referenced entries and tags need to be registered before freezing", e)
        } catch (t: Throwable) {
            isFrozen = false
            throw t
        }
    }
    
    private fun registerUnknownEntries() {
        check(lock.isHeldByCurrentThread)
        check(unknownEntryFactory != null || missingEntryKeys.isEmpty()) {
            "Entries were not re-registered and no unknown-entry factory is configured: ${missingEntryKeys.joinToString()}"
        }
        
        if (unknownEntryFactory == null)
            return
        
        for (key in missingEntryKeys.toList()) {
            set(key, unknownEntryFactory(get(key)))
        }
    }
    
    private fun buildFlattenedTagContentsProvider(): Provider<Map<Key, Set<RegistryEntry.Nova<T>>>> {
        check(lock.isHeldByCurrentThread)
        
        val keys = tagsByKey.keys.toList()
        val providers = keys.map(unflattenedTagEntriesByKey::getValue) // throws for undefined but referenced tags
        return combinedProvider(providers) { entrySets ->
            flattenTags(keys.zip(entrySets).toMap(LinkedHashMap()))
        }
    }
    
    private fun flattenTags(
        definitions: Map<Key, Set<NovaTagEntry<T>>>
    ): Map<Key, Set<RegistryEntry.Nova<T>>> {
        val entriesByTag: MutableMap<Key, MutableSet<RegistryEntry.Nova<T>>> =
            definitions.keys.associateWithTo(LinkedHashMap()) { LinkedHashSet() }
        val dependentsByTag: MutableMap<Key, MutableSet<Key>> = HashMap()
        
        // Collect direct entries and track dependencies between tags
        for ([tagKey, entries] in definitions) {
            val directEntries = entriesByTag.getValue(tagKey)
            for (entry in entries) {
                require(entry.registry == this) { "Cannot have tag entries from other registries" }
                when (entry) {
                    is NovaTagEntry.Direct<*> -> {
                        @Suppress("UNCHECKED_CAST")
                        directEntries += entry.entry as RegistryEntry.Nova<T>
                    }
                    
                    is NovaTagEntry.Tag<*> -> {
                        val referencedTagKey = entry.tag.tagKey
                        if (referencedTagKey !in definitions)
                            throw NoSuchElementException("No tag found for key $referencedTagKey")
                        dependentsByTag.getOrPut(referencedTagKey, ::LinkedHashSet) += tagKey
                    }
                }
            }
        }
        
        // Iteratively propagate entries to dependent tags
        val pending = LinkedHashSet(entriesByTag.keys)
        while (pending.isNotEmpty()) {
            val tagKey = pending.removeFirst()
            
            val entries = entriesByTag.getValue(tagKey)
            for (dependentKey in dependentsByTag[tagKey].orEmpty()) {
                if (entriesByTag.getValue(dependentKey).addAll(entries))
                    pending += dependentKey
            }
        }
        
        return entriesByTag.mapValues { it.value.toSet() }
    }
    
    /**
     * Gets or puts a provider into the map under [key], which is expected to be initialized while holding
     * the lock on registry freeze.
     */
    private fun <K, V> MutableMap<K, Provider<V>>.getOrPutEagerProvider(key: K, lazyValue: () -> V): Provider<V> =
        getOrPut(key) {
            createProvider {
                // cannot resolve values pre-freeze
                checkFrozen()
                
                // Resolve logic may access internal state and as such requires holding the lock.
                // In practice, this means that providers need to be resolved on freeze().
                check(lock.isHeldByCurrentThread)
                
                lazyValue()
            }
        }
    
    /**
     * Gets or puts a provider into the map under [key], which is expected to be initialized lazily outside of
     * this registry's lock. Acquires the lock on value resolution. Used for optional providers.
     */
    private fun <K, V> MutableMap<K, Provider<V>>.getOrPutLazyProvider(key: K, lazyValue: () -> V): Provider<V> =
        getOrPut(key) {
            createProvider {
                check(!lock.isHeldByCurrentThread)
                lock.withLock {
                    // cannot resolve values pre-freeze
                    checkFrozen()
                    
                    lazyValue()
                }
            }
        }
    
    protected abstract fun <T> createProvider(lazyValue: () -> T): Provider<T>
    
    protected fun checkNotFrozen() = check(!isFrozen) { "Registry $this is frozen" }
    protected fun checkFrozen() = check(isFrozen) { "Registry $this is not ready (not yet frozen)" }
    
    final override fun hashCode() = key.hashCode()
    final override fun equals(other: Any?) = other === this || other is NovaRegistry<*> && key == other.key
    final override fun toString() = "NovaRegistry(key = $key)"
    
}

internal class ReloadableNovaRegistry<T : NovaRegistryElement<T>>(
    key: Key,
    unknownEntryFactory: ((RegistryEntry.Nova<T>) -> T)?
) : AbstractNovaRegistry<T>(key, unknownEntryFactory) {
    
    override val isReloadable: Boolean
        get() = true
    
    private var isReload = false
    
    override fun reload(configure: MutableNovaRegistry<T>.() -> Unit) {
        val entryValues: Map<MutableProvider<T>, DeferredValue.Direct<T>>
        val optionalEntryValues: Map<MutableProvider<RegistryEntry.Nova<T>?>, DeferredValue.Direct<RegistryEntry.Nova<T>?>>
        val optionalTagValues: Map<MutableProvider<RegistryEntrySet.Nova.Tag<T>?>, DeferredValue.Direct<RegistryEntrySet.Nova.Tag<T>?>>
        val allTags: DeferredValue.Direct<Set<RegistryEntrySet.Nova.Tag<T>>>
        val flattenedTagContentsProvider: Provider<Map<Key, Set<RegistryEntry.Nova<T>>>>
        
        lock.withLock {
            checkFrozen()
            check(!isReload) { "Registry is already reloading" }
            
            try {
                isReload = true
                val previousTagKeys = unflattenedTagEntriesByKey.keys - key
                reset()
                configure()
                
                for (tagKey in previousTagKeys) {
                    unflattenedTagEntriesByKey.putIfAbsent(tagKey, provider(emptySet()))
                }
                
                flattenedTagContentsProvider = freezeLocked()
                entryValues = entryProviders.entries.associate { [key, provider] ->
                    provider as MutableProvider<T>
                    provider to DeferredValue.Direct(getValueOrThrow(key))
                }
                optionalEntryValues = optionalEntryProviders.entries.associate { [key, provider] ->
                    provider as MutableProvider<RegistryEntry.Nova<T>?>
                    provider to DeferredValue.Direct(entries[key])
                }
                optionalTagValues = optionalTagProviders.entries.associate { [key, provider] ->
                    provider as MutableProvider<RegistryEntrySet.Nova.Tag<T>?>
                    provider to DeferredValue.Direct(tagsByKey[key])
                }
                allTags = DeferredValue.Direct(tagsByKey.values.toSet())
            } catch (e: NoSuchElementException) {
                isFrozen = false // freezing failed
                throw IllegalStateException("Referenced entries and tags need to be registered before freezing", e)
            } finally {
                isReload = false
            }
        }
        
        // update providers outside of lock as updating may run arbitrary code from observers
        entryValues.forEach { [provider, value] -> provider.update(value) }
        optionalEntryValues.forEach { [provider, value] -> provider.update(value) }
        tagProvidersHolder.set(flattenedTagContentsProvider)
        optionalTagValues.forEach { [provider, value] -> provider.update(value) }
        (tags as MutableProvider).update(allTags)
    }
    
    private fun reset() {
        check(lock.isHeldByCurrentThread)
        checkFrozen()
        
        isFrozen = false
        
        // remove all previous values
        missingEntryKeys += entryByKey.keys
        entryByKey.clear()
        keyByEntry.clear()
        entriesByName.clear()
        unflattenedTagEntriesByKey.clear()
    }
    
    override fun <T> createProvider(lazyValue: () -> T) = mutableProvider(lazyValue)
    
}

internal class StableNovaRegistry<T : NovaRegistryElement<T>>(
    key: Key,
    unknownEntryFactory: ((RegistryEntry.Nova<T>) -> T)?
) : AbstractNovaRegistry<T>(key, unknownEntryFactory) {
    
    override val isReloadable: Boolean
        get() = false
    
    override fun reload(configure: MutableNovaRegistry<T>.() -> Unit) {
        throw UnsupportedOperationException("Registry is not reloadable")
    }
    
    override fun <T> createProvider(lazyValue: () -> T) = provider(lazyValue)
    
}

private class UnmodifiableNovaRegistry<T : NovaRegistryElement<T>>(
    val mutableRegistry: MutableNovaRegistry<T>
) : NovaRegistry<T> by mutableRegistry {
    override fun hashCode() = key.hashCode()
    override fun equals(other: Any?) = other === this || other is NovaRegistry<*> && key == other.key
    override fun toString() = "NovaRegistry(key = $key)"
}
