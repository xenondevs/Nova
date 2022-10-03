@file:Suppress("UNCHECKED_CAST")

package xyz.xenondevs.nova.data.config.provider

import kotlin.reflect.KProperty

internal fun <T : Any, R> Provider<T>.map(transform: (T) -> R): Provider<R> {
    return MapEverythingProvider(this, transform).also(children::add)
}

@JvmName("map1")
internal fun <T, R> Provider<T>.map(transform: (T & Any) -> R): Provider<R?> {
    return MapOrNullProvider(this, transform).also(children::add)
}

internal fun <T> Provider<T>.orElse(value: T & Any): Provider<T & Any> {
    return FallbackValueProvider(this, value).also(children::add)
}

internal fun <T> Provider<T>.orElse(provider: Provider<T>): Provider<T> {
    return FallbackProviderProvider(this, provider)
}

internal abstract class Provider<T> {
    
    internal val children = ArrayList<Provider<*>>()
    private var initialized = false
    private var _value: T? = null
    
    val value: T
        get() {
            if (!initialized) {
                _value = loadValue()
                initialized = true
            }
            
            return _value as T
        }
    
    protected abstract fun loadValue(): T
    
    protected fun update() {
        _value = loadValue()
        children.forEach(Provider<*>::update)
    }
    
    operator fun getValue(thisRef: Any, property: KProperty<*>): T {
        return value
    }
    
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

private class FallbackProviderProvider<T>(
    private val provider: Provider<T>,
    private val fallback: Provider<T>
) : Provider<T>() {
    override fun loadValue(): T {
        return provider.value ?: fallback.value
    }
}
