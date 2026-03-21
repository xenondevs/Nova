package xyz.xenondevs.nova.config

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import xyz.xenondevs.commons.provider.MutableProvider
import xyz.xenondevs.commons.provider.mutableProvider
import xyz.xenondevs.nova.serialization.kotlinx.NOVA_SERIALIZERS_MODULE
import xyz.xenondevs.nova.util.data.readJson
import xyz.xenondevs.nova.util.data.writeJson
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.reflect.KType

internal object PermanentStorage {
    
    val JSON = Json {
        allowStructuredMapKeys = true
        serializersModule = NOVA_SERIALIZERS_MODULE
    }
    
    private val dir = Path("plugins/Nova/.internal_data/storage/")
    
    fun has(key: String): Boolean =
        getPath(key).exists()
    
    fun remove(key: String) =
        getPath(key).deleteIfExists()
    
    fun <T> store(key: String, serializer: SerializationStrategy<T>, data: T): Unit =
        getPath(key).also { it.createParentDirectories() }.writeJson(serializer, data, JSON)
    
    fun <T> retrieve(key: String, deserializer: DeserializationStrategy<T>): T? =
        getPath(key).takeIf { it.exists() }?.readJson(deserializer, JSON)
    
    @Suppress("UNCHECKED_CAST")
    fun <T> store(key: String, type: KType, data: T): Unit =
        store(key, JSON.serializersModule.serializer(type) as KSerializer<T>, data)
    
    @Suppress("UNCHECKED_CAST")
    fun <T> retrieve(key: String, type: KType): T? =
        retrieve(key, JSON.serializersModule.serializer(type) as KSerializer<T>)
    
    inline fun <reified T> store(key: String, data: T): Unit =
        getPath(key).also { it.createParentDirectories() }.writeJson(data, JSON)
    
    inline fun <reified T> retrieve(key: String): T? =
        getPath(key).takeIf { it.exists() }?.readJson(JSON)
    
    inline fun <reified T> storedValue(key: String, noinline alternativeProvider: () -> T): MutableProvider<T> =
        mutableProvider(
            { retrieve<T>(key) ?: alternativeProvider().also { store(key, it) } },
            { store<T>(key, it) }
        )
    
    fun <T> storedValue(key: String, serializer: KSerializer<T>, alternativeProvider: () -> T): MutableProvider<T> =
        mutableProvider(
            { retrieve(key, serializer) ?: alternativeProvider().also { store(key, serializer, it) } },
            { store(key, serializer, it) }
        )
    
    fun getPath(key: String): Path =
        dir.resolve("$key.json")
    
}