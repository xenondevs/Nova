package xyz.xenondevs.nova.data.config

import org.bukkit.configuration.file.YamlConfiguration
import xyz.xenondevs.nova.material.ItemNovaMaterial
import kotlin.reflect.KProperty

@Suppress("UNCHECKED_CAST", "LeakingThis")
internal abstract class ConfigAccess(private val configReceiver: () -> YamlConfiguration) {
    
     val cfg: YamlConfiguration
        get() = configReceiver()
    
    constructor(path: String) : this({ NovaConfig[path] })
    
    constructor(material: ItemNovaMaterial) : this({ NovaConfig[material] })
    
    protected fun <T : Any> getEntry(key: String): ConfigEntryAccessor<T> {
        return RequiredConfigEntryAccessor<T>(key)
            .also(ConfigEntryAccessor<*>::reload)
    }
    
    protected fun <T : Any> getEntry(key: String, default: T): ConfigEntryAccessor<T> {
        return DefaultConfigEntryAccessor(key, default)
            .also(ConfigEntryAccessor<*>::reload)
    }
    
    protected fun <T : Any> getOptionalEntry(key: String): ConfigEntryAccessor<T?> {
        return NullableConfigEntryAccessor<T>(key)
            .also(ConfigEntryAccessor<*>::reload)
    }
    
    protected fun <C : Any, R : Any> getEntry(key: String, mapValue: (C) -> R): ConfigEntryAccessor<R> {
        return RequiredMappingConfigEntryAccessor(key, mapValue)
            .also(ConfigEntryAccessor<*>::reload)
    }
    
    protected fun <C : Any, R : Any> getEntry(key: String, default: R, mapValue: (C) -> R): ConfigEntryAccessor<R> {
        return DefaultMappingConfigEntryAccessor(key, default, mapValue)
            .also(ConfigEntryAccessor<*>::reload)
    }
    
    protected fun <C : Any, R : Any> getOptionalEntry(key: String, mapValue: (C) -> R): ConfigEntryAccessor<R?> {
        return NullableMappingConfigEntryAccessor(key, mapValue)
            .also(ConfigEntryAccessor<*>::reload)
    }
    
    private inner class RequiredMappingConfigEntryAccessor<C : Any, R : Any>(
        key: String,
        private val mapValue: (C) -> R
    ) : ConfigEntryAccessor<R>(key) {
        override fun readValue(): R {
            return mapValue.invoke(cfg.get(key) as C)
        }
    }
    
    private inner class DefaultMappingConfigEntryAccessor<C : Any, R : Any>(
        key: String,
        private val default: R,
        private val mapValue: (C) -> R
    ) : ConfigEntryAccessor<R>(key) {
        override fun readValue(): R {
            return (cfg.get(key) as? C)?.let(mapValue) ?: default
        }
    }
    
    private inner class NullableMappingConfigEntryAccessor<C : Any, R : Any>(
        key: String,
        private val mapValue: (C) -> R
    ) : ConfigEntryAccessor<R?>(key) {
        override fun readValue(): R? {
            return (cfg.get(key) as? C)?.let(mapValue)
        }
    }
    
    private inner class RequiredConfigEntryAccessor<T : Any>(key: String) : ConfigEntryAccessor<T>(key) {
        override fun readValue(): T {
            check(key in cfg) { "No such config entry: $key"}
            return cfg.get(key) as T
        }
    }
    
    private inner class DefaultConfigEntryAccessor<T : Any>(key: String, private val default: T) : ConfigEntryAccessor<T>(key) {
        override fun readValue(): T {
            return cfg.get(key) as? T ?: default
        }
    }
    
    private inner class NullableConfigEntryAccessor<T : Any>(key: String) : ConfigEntryAccessor<T?>(key) {
        override fun readValue(): T? {
            return cfg.get(key) as? T
        }
    }
    
    protected abstract class ConfigEntryAccessor<T>(protected val key: String) : Reloadable {
        
        private var value: T? = null
        
        init {
            NovaConfig.reloadables += this
        }
        
        operator fun getValue(thisRef: Any, property: KProperty<*>): T = value as T
        
        override fun reload() {
            value = readValue()
        }
        
        abstract fun readValue(): T
        
    }
    
    
}