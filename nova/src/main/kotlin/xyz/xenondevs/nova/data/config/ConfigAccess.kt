@file:Suppress("unused", "UNCHECKED_CAST")

package xyz.xenondevs.nova.data.config

import org.bukkit.configuration.file.YamlConfiguration
import xyz.xenondevs.nova.data.provider.Provider
import xyz.xenondevs.nova.data.provider.orElse
import xyz.xenondevs.nova.data.provider.requireNonNull
import xyz.xenondevs.nova.material.ItemNovaMaterial
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

private val NUMBER_CONVERTER_MAP: Map<KClass<*>, (Number) -> Number> = mapOf(
    Byte::class to { it.toByte() },
    Short::class to { it.toShort() },
    Int::class to { it.toInt() },
    Long::class to { it.toLong() },
    Float::class to { it.toFloat() },
    Double::class to { it.toDouble() }
)

abstract class ConfigAccess(private val configReceiver: () -> YamlConfiguration) {
    
    val cfg: YamlConfiguration
        get() = configReceiver()
    
    constructor(path: String) : this({ NovaConfig[path] })
    
    constructor(material: ItemNovaMaterial) : this({ NovaConfig[material] })
    
    protected fun <T : Any> getEntry(key: String): Provider<T> {
        return RequiredConfigEntryAccessor<T>(key)
            .also(ConfigEntryAccessor<*>::reload)
    }
    
    protected fun <T : Any> getEntry(key: String, vararg fallbackKeys: String): Provider<T> {
        var provider: Provider<T?> = NullableConfigEntryAccessor(key)
        fallbackKeys.forEach { provider = provider.orElse(NullableConfigEntryAccessor(it)) }
        return provider
            .requireNonNull("No such config entries: $key, ${fallbackKeys.joinToString()}")
            .also(Provider<*>::update)
    }
    
    protected inline fun <reified T : Any> getOptionalEntry(key: String): Provider<T?> {
        val typeClass = T::class
        return getNullableConfigEntryAccessor<T>(typeClass, key)
            .also(Provider<*>::update)
    }
    
    protected inline fun <reified T : Any> getOptionalEntry(key: String, vararg fallbackKeys: String): Provider<T?> {
        val typeClass = T::class
        var provider: Provider<T?> = getNullableConfigEntryAccessor(typeClass, key)
        fallbackKeys.forEach { provider = provider.orElse(getNullableConfigEntryAccessor(typeClass, it)) }
        return provider.also(Provider<*>::update)
    }
    
    @PublishedApi
    internal fun <T : Any> getNullableConfigEntryAccessor(typeClass: KClass<*>, key: String): Provider<T?> {
        return if (typeClass.isSubclassOf(Number::class)) {
            val numberConverter = NUMBER_CONVERTER_MAP[typeClass] as (Number) -> T
            NullableConfigNumberEntryAccessor(key, numberConverter)
        } else NullableConfigEntryAccessor(key)
    }
    
    protected inner class RequiredConfigEntryAccessor<T : Any>(key: String) : ConfigEntryAccessor<T>(key) {
        override fun loadValue(): T {
            check(key in cfg) { "No such config entry: $key" }
            return cfg.get(key) as T
        }
    }
    
    protected inner class NullableConfigEntryAccessor<T : Any>(key: String) : ConfigEntryAccessor<T?>(key) {
        override fun loadValue(): T? {
            return cfg.get(key) as? T
        }
    }
    
    protected inner class NullableConfigNumberEntryAccessor<T : Any>(
        key: String,
        private val converter: (Number) -> T
    ) : ConfigEntryAccessor<T?>(key) {
        override fun loadValue(): T? {
            return (cfg.get(key) as? Number)?.let(converter)
        }
    }
    
    @Suppress("LeakingThis")
    protected abstract class ConfigEntryAccessor<T>(protected val key: String) : Provider<T>(), Reloadable {
        
        init {
            NovaConfig.reloadables += this
        }
        
        override fun reload() {
            update()
        }
        
    }
    
}