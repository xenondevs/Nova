package xyz.xenondevs.nova.resources.lookup

import net.kyori.adventure.key.Key
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.combinedProvider
import xyz.xenondevs.commons.provider.map
import xyz.xenondevs.commons.provider.mutableProvider
import xyz.xenondevs.commons.reflection.createType
import xyz.xenondevs.nova.config.PermanentStorage
import xyz.xenondevs.nova.resources.ResourcePath
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.typeOf

internal open class ResourceLookup<T : Any>(
    val key: String,
    private val type: KType,
    empty: T,
) {
    
    val provider = mutableProvider(empty)
    val value by provider
    
    internal open fun set(value: T) {
        PermanentStorage.store(key, value)
        provider.set(value)
    }
    
    internal fun load() {
        val loaded = PermanentStorage.retrieveOrNull<T>(type, key)
        if (loaded != null)
            set(loaded)
    }
    
    operator fun getValue(thisRef: Any, property: KProperty<*>): T = value
    
    internal open operator fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        set(value)
    }
    
}

internal class MapResourceLookup<K : Any, V : Any>(
    key: String,
    typeK: KType,
    typeV: KType
) : ResourceLookup<Map<K, V>>(key, HashMap::class.createType(typeK, typeV), emptyMap()) {
    
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
    type: KType
) : ResourceLookup<Map<String, T>>(key, HashMap::class.createType(typeOf<String>(), type), emptyMap()) {
    
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