package xyz.xenondevs.nova.data.provider

fun <T> provider(value: T): Provider<T> =
    ProviderWrapper(value)

fun <T> combinedProvider(vararg providers: Provider<out T>): Provider<List<T>> =
    CombinedProvider(providers.asList())

fun <T> combinedProvider(providers: List<Provider<T>>): Provider<List<T>> =
    CombinedProvider(providers)

fun <T> lazyProvider(initializer: () -> Provider<T>): Provider<T> =
    LazyProvider(initializer)

fun <T> combinedLazyProvider(initializer: () -> List<Provider<T>>): Provider<List<T>> =
    LazyProvider { CombinedProvider(initializer()) }

private class ProviderWrapper<T>(private val staticValue: T) : Provider<T>() {
    override fun loadValue(): T {
        return staticValue
    }
}

private class CombinedProvider<T>(private val providers: List<Provider<out T>>) : Provider<List<T>>() {
    
    init {
        providers.forEach { it.addChild(this) }
    }
    
    override fun loadValue(): List<T> {
        return providers.map { it.value }
    }
    
}

private class LazyProvider<T>(private val initializer: () -> Provider<T>) : Provider<T>() {
    
    private var _provider: Provider<T>? = null
    private val provider: Provider<T>
        get() {
            if (_provider == null)
                _provider = initializer().also { it.addChild(this) }
            
            return _provider!!
        }
    
    override fun loadValue(): T = provider.value
    
}