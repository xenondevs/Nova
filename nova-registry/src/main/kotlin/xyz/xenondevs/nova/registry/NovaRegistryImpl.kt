package xyz.xenondevs.nova.registry

import net.kyori.adventure.key.Key
import xyz.xenondevs.commons.collections.mapToSet
import xyz.xenondevs.commons.provider.DeferredValue
import xyz.xenondevs.commons.provider.MutableProvider
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.mutableProvider
import xyz.xenondevs.commons.provider.provider
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal abstract class AbstractNovaRegistry<T : NovaRegistryElement<T>>(
    override val key: Key
) : MutableNovaRegistry<T> {
    
    final override val unmodifiableView: NovaRegistry<T>
        get() = UnmodifiableNovaRegistry(this)
    
    protected val lock = ReentrantLock()
    
    // storage: store raw values, cleared and repopulated on reload
    protected val entryByKey: MutableMap<Key, T> = LinkedHashMap()
    protected val keyByEntry: MutableMap<T, Key> = HashMap()
    protected val entriesByName: MutableMap<String, MutableList<T>> = HashMap()
    protected val unflattenedTagEntriesByKey: MutableMap<Key, Set<NovaTagEntry<T>>> = HashMap()
    protected val flattenedTagEntriesByKey: MutableMap<Key, Set<RegistryEntry.Nova<T>>> = HashMap()
    
    // providers: updated on reload, maps are greedy
    protected val entryProviders: MutableMap<Key, Provider<T>> = HashMap()
    protected val optionalEntryProviders: MutableMap<Key, Provider<RegistryEntry.Nova<T>?>> = HashMap()
    protected val tagProviders: MutableMap<Key, Provider<Set<RegistryEntry.Nova<T>>>> = HashMap()
    protected val optionalTagProviders: MutableMap<Key, Provider<RegistryEntrySet.Nova.Tag<T>?>> = HashMap()
    
    // caches: cached objects for types that wrap providers, updated implicitly on reload, maps are greedy
    protected val entries: MutableMap<Key, RegistryEntry.Nova<T>> = HashMap()
    protected val tags: MutableMap<Key, RegistryEntrySet.Nova.Tag<T>> = HashMap()
    
    protected var isFrozen = false
    
    final override operator fun set(key: Key, value: T): Unit = lock.withLock {
        checkNotFrozen()
        require(key !in entryByKey) { "$key is already registered" }
        require(value !in keyByEntry) { "$value is already registered with key ${keyByEntry[value]}" }
        
        entryByKey[key] = value
        keyByEntry[value] = key
        entriesByName.getOrPut(key.value()) { mutableListOf() } += value
    }
    
    final override fun set(tagKey: Key, entries: Set<NovaTagEntry<T>>): Unit = lock.withLock {
        checkNotFrozen()
        require(tagKey != key) { "Tag key cannot match registry key, as that is reserved for the entrySet tag" }
        require(tagKey !in unflattenedTagEntriesByKey) { "Tag $tagKey is already registered" }
        require(entries.all { it.registry == this }) { "Cannot have tag entries from other registries" }
        
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
            require(key in unflattenedTagEntriesByKey) { "Cannot create tag entry set for unregistered tag key $key after freezing" }
            return tags[key]!!
        } else {
            return tags.getOrPut(key) {
                val entries = tagProviders.getOrPutEagerProvider(key) {
                    flattenedTagEntriesByKey[key]
                        ?: throw NoSuchElementException("No tag found for key $key")
                }
                NovaTagRegistryEntrySet(this, key, entries)
            }
        }
    }
    
    final override fun getOptionalTag(key: Key): Provider<RegistryEntrySet.Nova.Tag<T>?> = lock.withLock {
        return optionalTagProviders.getOrPutLazyProvider(key) { tags[key] }
    }
    
    final override fun freeze(): Unit = lock.withLock {
        checkNotFrozen()
        
        // populate entries map & register "all entries" tag
        unflattenedTagEntriesByKey[key] = entryByKey.keys.mapToSet { NovaTagEntry.Direct(get(it)) }
        // populate tags map
        unflattenedTagEntriesByKey.keys.forEach { getTag(it) }
        
        buildTags()
        isFrozen = true
        
        try {
            // bind entry- and tag providers to their initial value
            entries.values.forEach { it.get() }
            tags.values.forEach { it.get() }
        } catch (e: NoSuchElementException) {
            isFrozen = false // freezing failed
            throw IllegalStateException("Referenced entries need to be registered before freezing", e)
        }
    }
    
    private fun buildTags() {
        check(lock.isHeldByCurrentThread)
        
        fun flatten(key: Key): Set<RegistryEntry.Nova<T>> {
            val memoized = flattenedTagEntriesByKey[key]
            if (memoized != null)
                return memoized
            
            val entries = unflattenedTagEntriesByKey[key]!!
            flattenedTagEntriesByKey[key] = emptySet() // prevent infinite recursion in case of cyclic dependencies
            val result = entries.flatMapTo(LinkedHashSet()) { entry ->
                when (entry) {
                    is NovaTagEntry.Direct -> setOf(entry.entry)
                    is NovaTagEntry.Tag -> flatten(entry.tag.tagKey)
                }
            }
            flattenedTagEntriesByKey[key] = result
            return result
        }
        
        unflattenedTagEntriesByKey.keys.forEach(::flatten)
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

internal class ReloadableNovaRegistry<T : NovaRegistryElement<T>>(key: Key) : AbstractNovaRegistry<T>(key) {
    
    override val isReloadable: Boolean
        get() = true
    
    private var isReload = false
    
    override fun reload(configure: MutableNovaRegistry<T>.() -> Unit) {
        val entryValues: Map<MutableProvider<T>, DeferredValue.Direct<T>>
        val optionalEntryValues: Map<MutableProvider<RegistryEntry.Nova<T>?>, DeferredValue.Direct<RegistryEntry.Nova<T>?>>
        val tagValues: Map<MutableProvider<Set<RegistryEntry.Nova<T>>>, DeferredValue.Direct<Set<RegistryEntry.Nova<T>>>>
        val optionalTagValues: Map<MutableProvider<RegistryEntrySet.Nova.Tag<T>?>, DeferredValue.Direct<RegistryEntrySet.Nova.Tag<T>?>>
        
        lock.withLock {
            checkFrozen()
            check(!isReload) { "Registry is already reloading" }
            
            try {
                isReload = true
                reset()
                configure()
                freeze()
                entryValues = entryProviders.entries.associate { (key, provider) ->
                    provider as MutableProvider<T>
                    provider to DeferredValue.Direct(getValueOrThrow(key))
                }
                optionalEntryValues = optionalEntryProviders.entries.associate { (key, provider) ->
                    provider as MutableProvider<RegistryEntry.Nova<T>?>
                    provider to DeferredValue.Direct(entries[key])
                }
                tagValues = tagProviders.entries.associate { (key, provider) ->
                    provider as MutableProvider<Set<RegistryEntry.Nova<T>>>
                    provider to DeferredValue.Direct(flattenedTagEntriesByKey[key]
                        ?: throw NoSuchElementException("No tag found for key $key"))
                }
                optionalTagValues = optionalTagProviders.entries.associate { (key, provider) ->
                    provider as MutableProvider<RegistryEntrySet.Nova.Tag<T>?>
                    provider to DeferredValue.Direct(tags[key])
                }
            } catch (e: NoSuchElementException) {
                isFrozen = false // freezing failed
                throw IllegalStateException("Referenced entries need to be registered before freezing", e)
            } finally {
                isReload = false
            }
        }
        
        // update providers outside of lock as updating may run arbitrary code from observers
        entryValues.forEach { (provider, value) -> provider.update(value) }
        optionalEntryValues.forEach { (provider, value) -> provider.update(value) }
        tagValues.forEach { (provider, value) -> provider.update(value) }
        optionalTagValues.forEach { (provider, value) -> provider.update(value) }
    }
    
    private fun reset() {
        check(lock.isHeldByCurrentThread)
        checkFrozen()
        
        isFrozen = false
        
        // remove all previous values
        entryByKey.clear()
        keyByEntry.clear()
        entriesByName.clear()
        unflattenedTagEntriesByKey.clear()
        flattenedTagEntriesByKey.clear()
    }
    
    override fun <T> createProvider(lazyValue: () -> T) = mutableProvider(lazyValue)
    
}

internal class StableNovaRegistry<T : NovaRegistryElement<T>>(key: Key) : AbstractNovaRegistry<T>(key) {
    
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