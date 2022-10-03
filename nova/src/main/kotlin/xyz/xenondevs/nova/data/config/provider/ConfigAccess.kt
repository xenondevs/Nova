package xyz.xenondevs.nova.data.config.provider

import org.bukkit.configuration.file.YamlConfiguration
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.config.Reloadable
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
    
    protected inline fun <reified T : Any> getOptionalEntry(key: String): Provider<T?> {
        val typeClass = T::class
        val accessor: ConfigEntryAccessor<T?> =
            if (typeClass.isSubclassOf(Number::class)) {
                NullableConfigNumberEntryAccessor(key, getNumberConverter(typeClass))
            } else NullableConfigEntryAccessor(key)
        
        accessor.reload()
        return accessor
    }
    
    internal fun <T> getNumberConverter(numberClass: KClass<*>): (Number) -> T {
        return NUMBER_CONVERTER_MAP[numberClass] as (Number) -> T
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
    
    protected inner class NullableConfigNumberEntryAccessor<T>(
        key: String,
        private val converter: (Number) -> T
    ) : ConfigEntryAccessor<T?>(key) {
        
        override fun loadValue(): T? {
            return (cfg.get(key) as? Number)?.let(converter)
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