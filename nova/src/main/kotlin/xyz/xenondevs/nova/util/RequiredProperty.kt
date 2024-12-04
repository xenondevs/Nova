package xyz.xenondevs.nova.util

internal class RequiredProperty<V : Any>(private val message: String) {
    
    private var value: V? = null
    
    operator fun getValue(thisRef: Any?, property: Any?): V {
        return value ?: throw IllegalStateException(message)
    }
    
    operator fun setValue(thisRef: Any?, property: Any?, value: V) {
        this.value = value
    }
    
}