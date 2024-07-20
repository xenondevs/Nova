package xyz.xenondevs.nova.data.resources.lookup

import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.commons.provider.immutable.provider
import xyz.xenondevs.commons.reflection.createType
import xyz.xenondevs.nova.data.config.PermanentStorage
import xyz.xenondevs.nova.data.resources.ResourcePath
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.typeOf

open class ResourceLookup<T : Any> internal constructor(
    val key: String,
    private val type: KType,
    empty: T,
) {
    
    var value: T = empty
    val provider = provider { value }
    
    internal open fun set(value: T) {
        this.value = value
        PermanentStorage.store(key, value)
        provider.update()
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

class IdResourceLookup<T : Any> internal constructor(
    key: String,
    type: KType
) : ResourceLookup<Map<String, T>>(key, HashMap::class.createType(typeOf<String>(), type), emptyMap()) {
    
    operator fun get(id: String): T? =
        value[id]
    
    operator fun get(id: ResourceLocation): T? =
        value[id.toString()]
    
    operator fun get(id: ResourcePath): T? =
        value[id.toString()]
    
    fun getOrThrow(id: String): T =
        value[id] ?: throw IllegalArgumentException("Resource lookup $key does not contain $id")
    
    fun getOrThrow(id: ResourceLocation): T =
        getOrThrow(id.toString())
    
    fun getOrThrow(id: ResourcePath): T =
        getOrThrow(id.toString())
    
    operator fun contains(id: String): Boolean =
        id in value
    
    operator fun contains(id: ResourceLocation): Boolean =
        id.toString() in value
    
    operator fun contains(id: ResourcePath): Boolean =
        id.toString() in value
    
    fun toMap(): HashMap<String, T> =
        HashMap(value)
    
    fun <R> toMap(mapValues: (T) -> R): HashMap<String, R> =
        value.entries.associateTo(HashMap()) { it.key to mapValues(it.value) }
    
    override fun set(value: Map<String, T>) {
        for (key in value.keys) {
            require(ResourcePath.NAMESPACED_ENTRY.matches(key)) { "Illegal key $key" }
        }
        
        super.set(value)
    }
    
    @JvmName("set1")
    fun set(value: Map<ResourcePath, T>) {
        super.set(value.mapKeysTo(HashMap()) { it.key.toString() })
    }
    
}