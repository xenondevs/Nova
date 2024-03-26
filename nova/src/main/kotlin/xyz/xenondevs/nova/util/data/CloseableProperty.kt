package xyz.xenondevs.nova.util.data

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

internal class CloseableProperty<V>(var value: V, val closeAction: (V) -> Unit) : ReadWriteProperty<Any, V> {
    
    override fun getValue(thisRef: Any, property: KProperty<*>): V {
        return value
    }
    
    override fun setValue(thisRef: Any, property: KProperty<*>, value: V) {
        this.value = value
    }
    
}