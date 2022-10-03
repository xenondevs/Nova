package xyz.xenondevs.nova.data.config.provider

import org.bukkit.configuration.file.YamlConfiguration
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.config.Reloadable
import xyz.xenondevs.nova.material.ItemNovaMaterial

@Suppress("UNCHECKED_CAST", "LeakingThis")
internal abstract class ConfigAccess(private val configReceiver: () -> YamlConfiguration) {
    
     val cfg: YamlConfiguration
        get() = configReceiver()
    
    constructor(path: String) : this({ NovaConfig[path] })
    
    constructor(material: ItemNovaMaterial) : this({ NovaConfig[material] })
    
    protected fun <T : Any> getEntry(key: String): Provider<T> {
        return RequiredConfigEntryAccessor<T>(key)
            .also(ConfigEntryAccessor<*>::reload)
    }
    
    protected fun <T : Any> getOptionalEntry(key: String): Provider<T?> {
        return NullableConfigEntryAccessor<T>(key)
            .also(ConfigEntryAccessor<*>::reload)
    }
    
    private inner class RequiredConfigEntryAccessor<T : Any>(key: String) : ConfigEntryAccessor<T>(key) {
        override fun loadValue(): T {
            check(key in cfg) { "No such config entry: $key"}
            return cfg.get(key) as T
        }
    }
    
    private inner class NullableConfigEntryAccessor<T : Any>(key: String) : ConfigEntryAccessor<T?>(key) {
        override fun loadValue(): T? {
            return cfg.get(key) as? T
        }
    }
    
    protected abstract class ConfigEntryAccessor<T>(protected val key: String) : Provider<T>(), Reloadable {
        
        init {
            NovaConfig.reloadables += this
        }
        
        override fun reload() {
            update()
        }
        
    }
    
}