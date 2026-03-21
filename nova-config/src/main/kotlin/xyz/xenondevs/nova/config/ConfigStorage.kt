package xyz.xenondevs.nova.config

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonBuilder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.overwriteWith
import kotlinx.serialization.serializer
import net.kyori.adventure.key.Key
import xyz.xenondevs.commons.provider.MutableProvider
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.UnstableProviderApi
import xyz.xenondevs.commons.provider.combinedProvider
import xyz.xenondevs.commons.provider.mutableProvider
import xyz.xenondevs.commons.provider.strongCombinedProvider
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KType

/**
 * Stores and manages [configs][ConfigProvider] identified by [Keys][Key].
 *
 * Configs are loaded from the given [ConfigBackend] and deserialized using `kotlinx.serialization`
 * with [rootSerializers] as the base [SerializersModule].
 * Each config is exposed as a [ConfigProvider], which provides reactive access to individual entries.
 */
class ConfigStorage(
    private val rootSerializers: SerializersModule,
    private val backend: ConfigBackend
) {
    
    private var jsons = ConcurrentHashMap<String, Json>()
    private val configProviders = ConcurrentHashMap<Key, ConfigProviderImpl>()
    private val readTimes = ConcurrentHashMap<Key, Long>()
    
    /**
     * Sets the additional [serializers] to use for configs in [namespace].
     * These are merged on top of [rootSerializers], with the namespace-specific serializers taking precedence.
     */
    fun setSerializers(namespace: String, serializers: SerializersModule) {
        require(Key.parseableNamespace(namespace)) { "Invalid namespace: $namespace" }
        jsons[namespace] = createJson {
            serializersModule = rootSerializers.overwriteWith(serializers)
        }
    }
    
    /**
     * Gets or creates the [ConfigProvider] for the config identified by [id].
     */
    operator fun get(id: String): ConfigProvider =
        get(Key.key(id))
    
    /**
     * Gets or creates the [ConfigProvider] for the config identified by [id].
     */
    operator fun get(id: Key): ConfigProvider = configProviders.computeIfAbsent(id) {
        readTimes[id] = System.currentTimeMillis()
        val cfg = backend.load(id) ?: JsonObject(emptyMap())
        ConfigProviderImpl(it, mutableProvider(cfg))
    }
    
    /**
     * Reloads all configs that have been modified since the last reload
     * and returns the set of config ids that were reloaded.
     */
    fun reload(): Set<Key> {
        val reloaded = mutableSetOf<Key>()
        synchronized(this) {
            for ((id, provider) in configProviders) {
                val lastModified = backend.getLastModified(id)
                if (lastModified <= (readTimes[id] ?: 0L))
                    continue
                
                readTimes[id] = System.currentTimeMillis()
                provider.delegate.set(backend.load(id) ?: continue)
                
                reloaded += id
            }
        }
        
        resolveEntries()
        backend.postReload()
        return reloaded
    }
    
    /**
     * Resolves all [config entries][ConfigProvider.entry], 
     * causing serialization errors to be reported to the [backend][ConfigBackend.onError].
     */
    fun resolveEntries() {
        for ((_, provider) in configProviders) {
            provider.resolveEntries()
        }
    }
    
    private fun getJson(namespace: String): Json =
        jsons.computeIfAbsent(namespace) { createJson { serializersModule = rootSerializers } }
    
    private fun createJson(builder: JsonBuilder.() -> Unit) = Json {
        ignoreUnknownKeys = true
        decodeEnumsCaseInsensitive = true
        builder()
    }
    
    @OptIn(UnstableProviderApi::class)
    private inner class ConfigProviderImpl(
        override val configId: Key,
        override val delegate: MutableProvider<JsonElement>
    ) : ConfigProvider, Provider<JsonElement> by delegate {
        
        private val entries: MutableSet<Provider<*>> =
            Collections.synchronizedSet(Collections.newSetFromMap(WeakHashMap()))
        
        override fun node(path: List<String>): ConfigProvider {
            return SubProvider(path, entry(JsonObject(emptyMap()), path))
        }
        
        override fun <T : Any> entry(type: KType, default: Provider<T>, vararg paths: List<String>): Provider<T> {
            val provider = combinedProvider(this, default) { cfg, default ->
                deserializeFirstOrNull(cfg, type, *paths) ?: default
            }
            entries += provider
            return provider
        }
        
        override fun <T : Any> optionalEntry(type: KType, vararg paths: List<String>): Provider<T?> {
            val provider = map { deserializeFirstOrNull<T>(it, type, *paths) }
            entries += provider
            return provider
        }
        
        override fun strongNode(path: List<String>): ConfigProvider {
            return SubProvider(path, strongEntry(JsonObject(emptyMap()), path))
        }
        
        override fun <T : Any> strongEntry(type: KType, default: Provider<T>, vararg paths: List<String>): Provider<T> {
            val provider = strongCombinedProvider(this, default) { cfg, default ->
                deserializeFirstOrNull(cfg, type, *paths) ?: default
            }
            entries += provider
            return provider
        }
        
        override fun <T : Any> strongOptionalEntry(type: KType, vararg paths: List<String>): Provider<T?> {
            val provider = strongMap { deserializeFirstOrNull<T>(it, type, *paths) }
            entries += provider
            return provider
        }
        
        fun resolveEntries() {
            synchronized(entries) {
                entries.forEach { it.get() }
            }
        }
        
        private fun <T : Any> deserializeFirstOrNull(el: JsonElement, type: KType, vararg paths: List<String>): T? {
            val json = getJson(configId.namespace())
            for (path in paths) {
                val element = el.resolve(path)
                    ?: continue
                
                @Suppress("UNCHECKED_CAST")
                val serializer = json.serializersModule.serializer(type) as KSerializer<T>
                try {
                    return json.decodeFromJsonElement(serializer, element)
                } catch (e: SerializationException) {
                    backend.onError(configId, path, e)
                    return null
                }
            }
            return null
        }
        
        private fun JsonElement.resolve(path: List<String>): JsonElement? {
            var current: JsonElement = this
            for (segment in path) {
                if (current !is JsonObject)
                    return null
                current = current[segment] ?: return null
            }
            return current
        }
        
        private inner class SubProvider(
            private val pathPrefix: List<String>,
            override val delegate: Provider<JsonElement>
        ) : ConfigProvider, Provider<JsonElement> by delegate {
            
            override val configId: Key
                get() = this@ConfigProviderImpl.configId
            
            override fun node(path: List<String>): ConfigProvider =
                this@ConfigProviderImpl.node(pathPrefix + path)
            
            override fun <T : Any> entry(type: KType, default: Provider<T>, vararg paths: List<String>): Provider<T> =
                this@ConfigProviderImpl.entry(type, default, *paths.map { pathPrefix + it }.toTypedArray())
            
            override fun <T : Any> optionalEntry(type: KType, vararg paths: List<String>): Provider<T?> =
                this@ConfigProviderImpl.optionalEntry(type, *paths.map { pathPrefix + it }.toTypedArray())
            
            override fun strongNode(path: List<String>): ConfigProvider =
                this@ConfigProviderImpl.strongNode(pathPrefix + path)
            
            override fun <T : Any> strongEntry(type: KType, default: Provider<T>, vararg paths: List<String>): Provider<T> =
                this@ConfigProviderImpl.strongEntry(type, default, *paths.map { pathPrefix + it }.toTypedArray())
            
            override fun <T : Any> strongOptionalEntry(type: KType, vararg paths: List<String>): Provider<T?> =
                this@ConfigProviderImpl.strongOptionalEntry(type, *paths.map { pathPrefix + it }.toTypedArray())
            
        }
        
    }
    
}
