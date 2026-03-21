package xyz.xenondevs.nova.resources.lookup

import xyz.xenondevs.commons.provider.MutableProvider
import xyz.xenondevs.nova.config.PermanentStorage
import kotlin.reflect.KType

internal class ResourceLookup<T : Any>(
    val key: String,
    val type: KType,
    val provider: MutableProvider<T>,
    val default: T?
) {
    
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
        val value = PermanentStorage.retrieve(key, type)
            ?: default
            ?: throw IllegalStateException("Resource lookup '$key' is not present and has no default value")
        provider.set(value)
    }

    fun store() {
        PermanentStorage.store(key, type, provider.get())
    }

    fun remove() {
        PermanentStorage.remove(key)
    }

}