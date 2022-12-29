@file:Suppress("unused", "UNCHECKED_CAST")

package xyz.xenondevs.nova.data.config

import org.bukkit.configuration.file.YamlConfiguration
import xyz.xenondevs.nova.data.provider.Provider
import xyz.xenondevs.nova.data.provider.orElse
import xyz.xenondevs.nova.data.provider.requireNonNull
import xyz.xenondevs.nova.data.serialization.yaml.getLazilyEvaluated
import xyz.xenondevs.nova.material.ItemNovaMaterial
import xyz.xenondevs.nova.util.reflection.type
import java.lang.reflect.Type

abstract class ConfigAccess(private val configReceiver: () -> YamlConfiguration) {
    
    val cfg: YamlConfiguration
        get() = configReceiver()
    
    constructor(path: String) : this({ NovaConfig[path] })
    
    constructor(material: ItemNovaMaterial) : this({ NovaConfig[material] })
    
    protected inline fun <reified T : Any> getEntry(key: String): Provider<T> {
        return RequiredConfigEntryAccessor<T>(key, type<T>()).also(ConfigEntryAccessor<*>::reload)
    }
    
    protected inline fun <reified T : Any> getEntry(key: String, vararg fallbackKeys: String): Provider<T> {
        val type = type<T>()
        var provider: Provider<T?> = NullableConfigEntryAccessor(key, type)
        fallbackKeys.forEach { provider = provider.orElse(NullableConfigEntryAccessor(it, type)) }
        return provider
            .requireNonNull("No such config entries: $key, ${fallbackKeys.joinToString()}")
            .also(Provider<*>::update)
    }
    
    protected inline fun <reified T : Any> getOptionalEntry(key: String): Provider<T?> {
        return NullableConfigEntryAccessor<T>(key, type<T>()).also(Provider<*>::update)
    }
    
    protected inline fun <reified T : Any> getOptionalEntry(key: String, vararg fallbackKeys: String): Provider<T?> {
        val type = type<T>()
        var provider: Provider<T?> = NullableConfigEntryAccessor(key, type)
        fallbackKeys.forEach { provider = provider.orElse(NullableConfigEntryAccessor(it, type)) }
        return provider.also(Provider<*>::update)
    }
    
    protected inner class RequiredConfigEntryAccessor<T : Any>(key: String, type: Type) : ConfigEntryAccessor<T>(key, type) {
        override fun loadValue(): T {
            check(key in cfg) { "No such config entry: $key" }
            return cfg.getLazilyEvaluated(key, type)!!
        }
    }
    
    protected inner class NullableConfigEntryAccessor<T : Any>(key: String, type: Type) : ConfigEntryAccessor<T?>(key, type) {
        override fun loadValue(): T? {
            return cfg.getLazilyEvaluated(key, type)
        }
    }
    
    @Suppress("LeakingThis")
    protected abstract class ConfigEntryAccessor<T>(protected val key: String, protected val type: Type) : Provider<T>(), Reloadable {
        
        init {
            NovaConfig.reloadables += this
        }
        
        override fun reload() {
            update()
        }
        
    }
    
}