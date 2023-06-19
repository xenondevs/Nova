package xyz.xenondevs.nova.util.data

import kotlin.reflect.KProperty

internal class CentralizedLazy<T : Any>(private val initializer: () -> Unit) {
    
    private var value: T? = null
    
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        if (value == null)
            initializer()
        
        return value as T
    }
    
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        this.value = value
    }
    
}