package xyz.xenondevs.nova.config

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import kotlinx.serialization.serializer
import xyz.xenondevs.commons.provider.MutableProvider
import xyz.xenondevs.commons.provider.mutableProvider
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.util.AsyncExecutor
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.inputStream
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.outputStream
import kotlin.io.path.relativeTo
import kotlin.io.path.walk

private sealed interface Write {
    
    val key: String
    
    data class Store(override val key: String, val element: JsonElement) : Write
    data class Remove(override val key: String) : Write
    
}

internal object PermanentStorage {
    
    private val json = Json { allowStructuredMapKeys = true }
    private val dir = Path("plugins/Nova/.internal_data/storage/")
    private val files = ConcurrentHashMap<String, JsonElement>()
    private val writes = Channel<Write>(Channel.UNLIMITED)
    
    private val writer: Job = CoroutineScope(AsyncExecutor.SUPERVISOR + Dispatchers.IO).launch {
        for (write in writes) {
            val path = getPath(write.key)
            try {
                when (write) {
                    is Write.Store -> writeAtomically(path, write.element)
                    is Write.Remove -> path.deleteIfExists()
                }
            } catch (e: Exception) {
                LOGGER.error("Failed to update permanent storage file: $path", e)
            }
        }
    }
    
    private fun writeAtomically(path: Path, element: JsonElement) {
        path.createParentDirectories()
        val temp = path.resolveSibling(path.name + ".temp")
        try {
            temp.outputStream().use { json.encodeToStream(JsonElement.serializer(), element, it) }
            Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } finally {
            temp.deleteIfExists()
        }
    }
    
    suspend fun load() = withContext(Dispatchers.IO) {
        if (!dir.exists())
            return@withContext
        dir.walk()
            .filter { it.extension == "json" }
            .mapTo(ArrayList()) { path ->
                async {
                    if (!path.isRegularFile())
                        return@async null
                    val key = path.relativeTo(dir).invariantSeparatorsPathString.removeSuffix(".json")
                    val json = path.inputStream().use { json.decodeFromStream(JsonElement.serializer(), it) }
                    key to json
                }
            }
            .awaitAll()
            .filterNotNull()
            .forEach { [key, element] -> files[key] = element }
    }
    
    fun has(key: String): Boolean = files.containsKey(key)
    
    fun <T> retrieve(key: String, deserializer: DeserializationStrategy<T>): T? =
        files[key]?.let { json.decodeFromJsonElement(deserializer, it) }
    
    inline fun <reified T> store(key: String, data: T) =
        store(key, serializer<T>(), data)
    
    inline fun <reified T> retrieve(key: String): T? =
        retrieve(key, serializer<T>())
    
    inline fun <reified T> storedValue(key: String, noinline alternativeProvider: () -> T): MutableProvider<T> =
        storedValue(key, serializer<T>(), alternativeProvider)
    
    fun <T> storedValue(key: String, serializer: KSerializer<T>, alternativeProvider: () -> T): MutableProvider<T> =
        mutableProvider(
            { retrieve(key, serializer) ?: alternativeProvider().also { store(key, serializer, it) } },
            { store(key, serializer, it) }
        )
    
    fun <T> store(key: String, serializer: SerializationStrategy<T>, data: T) {
        queueStore(key, json.encodeToJsonElement(serializer, data))
    }
    
    @Synchronized
    private fun queueStore(key: String, element: JsonElement) {
        check(writes.trySend(Write.Store(key, element)).isSuccess) { "Permanent storage is closed" }
        files[key] = element
    }
    
    @Synchronized
    fun remove(key: String) {
        check(writes.trySend(Write.Remove(key)).isSuccess) { "Permanent storage is closed" }
        files.remove(key)
    }
    
    suspend fun shutdownAndWait() {
        synchronized(this) { writes.close() }
        writer.join()
    }
    
    private fun getPath(key: String): Path = dir.resolve("$key.json")
    
}