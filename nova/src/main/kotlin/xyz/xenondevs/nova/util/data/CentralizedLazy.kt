@file:Suppress("UNCHECKED_CAST")

package xyz.xenondevs.nova.util.data

import kotlin.reflect.KProperty

internal class CentralizedLazy<T>(private val initializer: () -> Unit) {
    
    private var initialized = false
    private var value: T? = null
    
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        if (!initialized) {
            initializer()
            if (!initialized)
                throw IllegalStateException("Initializer for CentralizedLazy did not initialize value")
        }
        
        return value as T
    }
    
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        this.value = value
        initialized = true
    }
    
}