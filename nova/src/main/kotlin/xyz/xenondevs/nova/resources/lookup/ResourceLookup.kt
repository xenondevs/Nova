package xyz.xenondevs.nova.resources.lookup

import net.kyori.adventure.key.Key
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.combinedProvider
import xyz.xenondevs.commons.provider.map
import xyz.xenondevs.commons.provider.mutableProvider
import xyz.xenondevs.nova.resources.ResourcePath
import kotlin.reflect.KProperty

internal open class ResourceLookup<T : Any>(
    val key: String,
    private val loadFn: (String) -> T?,
    private val storeFn: (String, T) -> Unit,
    empty: T,
) {
    
    val provider = mutableProvider(empty)
    val value by provider
    
    internal open fun set(value: T) {
        storeFn(key, value)
        provider.set(value)
    }
    
    internal fun load() {
        println("Loading lookup $key")
        val loaded = loadFn(key)
        if (loaded != null)
            provider.set(loaded)
    }
    
    operator fun getValue(thisRef: Any, property: KProperty<*>): T = value
    
    internal open operator fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        set(value)
    }
    
}

internal class MapResourceLookup<K : Any, V : Any>(
    key: String,
    loadFn: (String) -> Map<K, V>?,
    storeFn: (String, Map<K, V>) -> Unit,
) : ResourceLookup<Map<K, V>>(key, loadFn, storeFn, emptyMap()) {
    
    operator fun get(key: K): V? =
        value[key]
    
    fun getOrThrow(key: K): V =
        value[key] ?: throw IllegalArgumentException("Resource lookup ${this.key} does not contain $key")
    
    fun getProvider(key: K): Provider<V?> =
        provider.map { it[key] }
    
    fun getProvider(keyProvider: Provider<K?>): Provider<V?> =
        combinedProvider(provider, keyProvider) { map, key -> key?.let(map::get) }
    
}

internal class IdResourceLookup<T : Any>(
    key: String,
    loadFn: (String) -> Map<String, T>?,
    storeFn: (String, Map<String, T>) -> Unit,
) : ResourceLookup<Map<String, T>>(key, loadFn, storeFn, emptyMap()) {
    
    operator fun get(id: String): T? =
        value[id]
    
    operator fun get(id: Key): T? =
        value[id.toString()]
    
    fun getOrThrow(id: String): T =
        value[id] ?: throw IllegalArgumentException("Resource lookup $key does not contain $id")
    
    fun getOrThrow(id: Key): T =
        getOrThrow(id.toString())
    
    operator fun contains(id: String): Boolean =
        id in value
    
    operator fun contains(id: Key): Boolean =
        id.toString() in value
    
    fun toMap(): HashMap<String, T> =
        HashMap(value)
    
    fun <R> toMap(mapValues: (T) -> R): HashMap<String, R> =
        value.entries.associateTo(HashMap()) { it.key to mapValues(it.value) }
    
    override fun set(value: Map<String, T>) {
        for (key in value.keys) {
            require(ResourcePath.isValid(key)) { "Illegal key $key" }
        }
        
        super.set(value)
    }
    
    @JvmName("set1")
    fun set(value: Map<Key, T>) {
        super.set(value.mapKeysTo(HashMap()) { it.key.toString() })
    }
    
}