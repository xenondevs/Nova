@file:Suppress("unused", "UNCHECKED_CAST")

package xyz.xenondevs.nova.data.config

import org.bukkit.configuration.file.YamlConfiguration
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.immutable.orElse
import xyz.xenondevs.commons.provider.immutable.requireNonNull
import xyz.xenondevs.nova.data.serialization.yaml.getDeserialized
import xyz.xenondevs.nova.material.NovaItem
import kotlin.reflect.KType
import kotlin.reflect.typeOf

abstract class ConfigAccess(private val configReceiver: () -> YamlConfiguration) {
    
    val cfg: YamlConfiguration
        get() = configReceiver()
    
    constructor(path: String) : this({ NovaConfig[path] })
    
    constructor(material: NovaItem) : this({ NovaConfig[material] })
    
    protected inline fun <reified T : Any> getEntry(key: String): Provider<T> {
        return RequiredConfigEntryAccessor<T>(key, typeOf<T>()).also(ConfigEntryAccessor<*>::reload)
    }
    
    protected inline fun <reified T : Any> getEntry(key: String, vararg fallbackKeys: String): Provider<T> {
        val type = typeOf<T>()
        var provider: Provider<T?> = NullableConfigEntryAccessor(key, type)
        fallbackKeys.forEach { provider = provider.orElse(NullableConfigEntryAccessor(it, type)) }
        return provider
            .requireNonNull("No such config entries: $key, ${fallbackKeys.joinToString()}")
            .also(Provider<*>::update)
    }
    
    protected inline fun <reified T : Any> getOptionalEntry(key: String): Provider<T?> {
        return NullableConfigEntryAccessor<T>(key, typeOf<T>()).also(Provider<*>::update)
    }
    
    protected inline fun <reified T : Any> getOptionalEntry(key: String, vararg fallbackKeys: String): Provider<T?> {
        val type = typeOf<T>()
        var provider: Provider<T?> = NullableConfigEntryAccessor(key, type)
        fallbackKeys.forEach { provider = provider.orElse(NullableConfigEntryAccessor(it, type)) }
        return provider.also(Provider<*>::update)
    }
    
    protected inner class RequiredConfigEntryAccessor<T : Any>(key: String, type: KType) : ConfigEntryAccessor<T>(key, type) {
        override fun loadValue(): T {
            check(key in cfg) { "No such config entry: $key" }
            return cfg.getDeserialized(key, type)!!
        }
    }
    
    protected inner class NullableConfigEntryAccessor<T : Any>(key: String, type: KType) : ConfigEntryAccessor<T?>(key, type) {
        override fun loadValue(): T? {
            return cfg.getDeserialized(key, type)
        }
    }
    
    @Suppress("LeakingThis")
    protected abstract class ConfigEntryAccessor<T>(protected val key: String, protected val type: KType) : Provider<T>(), Reloadable {
        
        init {
            NovaConfig.reloadables += this
        }
        
        override fun reload() {
            update()
        }
        
    }
    
}