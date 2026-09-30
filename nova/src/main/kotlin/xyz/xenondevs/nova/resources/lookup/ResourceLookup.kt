package xyz.xenondevs.nova.resources.lookup

import kotlinx.serialization.KSerializer
import xyz.xenondevs.commons.provider.MutableProvider
import xyz.xenondevs.nova.config.PermanentStorage

internal class ResourceLookup<T : Any>(
    val key: String,
    val serializer: KSerializer<T>,
    val provider: MutableProvider<T>,
    val default: T?
) {
    
    @Volatile
    private var isLoaded = false
    
    init {
        provider.observe { isLoaded = true }
    }
    
    fun exists(): Boolean {
        return PermanentStorage.has(key)
    }
    
    fun load() {
        if (isLoaded)
            return
        val value = PermanentStorage.retrieve(key, serializer)
            ?: default
            ?: throw IllegalStateException("Resource lookup '$key' is not present and has no default value")
        provider.set(value)
    }
    
    fun store() {
        PermanentStorage.store(key, serializer, provider.get())
    }
    
    fun remove() {
        PermanentStorage.remove(key)
    }
    
}