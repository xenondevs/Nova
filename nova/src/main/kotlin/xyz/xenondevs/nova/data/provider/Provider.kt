@file:Suppress("UNCHECKED_CAST")

package xyz.xenondevs.nova.data.provider

import kotlin.reflect.KProperty

fun <T : Any, R> Provider<T>.map(transform: (T) -> R): Provider<R> {
    return MapEverythingProvider(this, transform).also(::addChild)
}

@JvmName("map1")
fun <T, R> Provider<T?>.map(transform: (T & Any) -> R): Provider<R?> {
    return MapOrNullProvider(this, transform).also(::addChild)
}

fun <T, R : T & Any> Provider<T?>.orElse(value: R): Provider<R> {
    return FallbackValueProvider(this, value).also(::addChild)
}

fun <T, R : T & Any> Provider<T?>.orElse(provider: Provider<R>): Provider<R> {
    return FallbackProviderProvider(this, provider).also(::addChild)
}

fun <T, R> Provider<List<T>>.flatMap(transform: (T) -> List<R>): Provider<List<R>> {
    return FlatMapProvider(this, transform).also(::addChild)
}

fun <T> Provider<List<List<T>>>.flatten(): Provider<List<T>> {
    return FlatMapProvider(this) { it }.also(::addChild)
}

abstract class Provider<T> {
    
    private var children: ArrayList<Provider<*>>? = null
    
    private var initialized = false
    private var _value: T? = null
    
    open var value: T
        get() {
            if (!initialized) {
                _value = loadValue()
                initialized = true
            }
            
            return _value as T
        }
        protected set(value) {
            initialized = true
            _value = value
        }
    
    fun update() {
        _value = loadValue()
        children?.forEach(Provider<*>::update)
    }
    
    fun addChild(provider: Provider<*>) {
        if (children == null)
            children = ArrayList(1)
        
        children!!.add(provider)
    }
    
    operator fun getValue(thisRef: Any?, property: KProperty<*>?): T = value
    
    protected abstract fun loadValue(): T
    
}

private class MapEverythingProvider<T, R>(
    private val provider: Provider<T>,
    private val transform: (T) -> R
) : Provider<R>() {
    override fun loadValue(): R {
        return transform(provider.value)
    }
}

private class MapOrNullProvider<T, R>(
    private val provider: Provider<T>,
    private val transform: (T & Any) -> R
) : Provider<R?>() {
    override fun loadValue(): R? {
        return provider.value?.let(transform)
    }
}

private class FallbackValueProvider<T, R : T & Any>(
    private val provider: Provider<T>,
    private val fallback: R
) : Provider<R>() {
    override fun loadValue(): R {
        return (provider.value ?: fallback) as R
    }
}

private class FallbackProviderProvider<T, R : T & Any>(
    private val provider: Provider<T?>,
    private val fallback: Provider<R>
) : Provider<R>() {
    override fun loadValue(): R {
        return (provider.value ?: fallback.value) as R
    }
}

private class FlatMapProvider<T, R>(
    private val provider: Provider<List<T>>,
    private val transform: (T) -> List<R>
) : Provider<List<R>>() {
    override fun loadValue(): List<R> {
        return provider.value.flatMap(transform)
    }
}