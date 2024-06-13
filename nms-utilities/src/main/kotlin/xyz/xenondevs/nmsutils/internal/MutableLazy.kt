package xyz.xenondevs.nmsutils.internal

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

internal class MutableLazy<T>(
    private val setHandler: (() -> Unit)? = null,
    private val initializer: () -> T
) : ReadWriteProperty<Any, T> {
    
    private var value: T? = null
    
    var isInitialized = false
        private set
    
    @Suppress("UNCHECKED_CAST")
    override fun getValue(thisRef: Any, property: KProperty<*>): T {
        if (!isInitialized) {
            value = initializer()
            isInitialized = true
        }
        return value as T
    }
    
    override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        this.value = value
        isInitialized = true
        setHandler?.invoke()
    }
    
}