package xyz.xenondevs.nova.registry

import xyz.xenondevs.commons.provider.MutableProvider
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.mutableProvider

internal class RegistryEntryMap<K : RegistryEntry<*>, V : RegistryEntry<*>> {
    
    private val entryProviders = HashMap<K, MutableProvider<V?>>()
    
    private fun getInternal(key: K): MutableProvider<V?> =
        entryProviders.getOrPut(key) { mutableProvider(null) }
    
    operator fun set(key: K, value: V?): Unit =
        getInternal(key).set(value)
    
    operator fun get(key: K): Provider<V?> =
        getInternal(key)
    
}