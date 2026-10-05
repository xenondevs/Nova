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
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.serializer
import net.kyori.adventure.key.Key
import xyz.xenondevs.commons.provider.MutableProvider
import xyz.xenondevs.commons.provider.mutableProvider
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.registry.KnownRegistryEntries.BlockConfiguration
import xyz.xenondevs.nova.serialization.kotlinx.KeySerializer
import xyz.xenondevs.nova.serialization.kotlinx.ResourceKeySerializer
import xyz.xenondevs.nova.util.AsyncExecutor
import xyz.xenondevs.nova.util.toKey
import xyz.xenondevs.nova.world.format.legacy.world.v2.LegacyBlockStateIdResolver
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
        
        migrateLegacy()
    }
    
    private fun migrateLegacy() {
        if (!has("block_state_id_map") && !has("custom_enchantment_ids") && !has("custom_entity_variant_keys"))
            return
        
        LOGGER.info("Performing PermanentStorage legacy migration...")
        
        val entriesSerializer = MapSerializer(KeySerializer, SetSerializer(KeySerializer))
        val statesSerializer = MapSerializer(KeySerializer, BlockConfiguration.serializer())
        
        val legacyVanillaBlocks = setOf(
            "nova:note_block", "nova:tripwire",
            "nova:oak_leaves", "nova:spruce_leaves", "nova:birch_leaves", "nova:jungle_leaves",
            "nova:acacia_leaves", "nova:dark_oak_leaves", "nova:mangrove_leaves", "nova:cherry_leaves",
            "nova:azalea_leaves", "nova:flowering_azalea_leaves", "nova:pale_oak_leaves"
        )
        
        val entries = retrieve("known_registry_entries", entriesSerializer)
            ?.mapValuesTo(HashMap()) { [_, keys] -> keys.toMutableSet() }
            ?: HashMap()
        val states = retrieve("known_block_states", statesSerializer)
            ?.toMutableMap()
            ?: HashMap()
        
        files["block_state_id_map"]?.let { element ->
            val legacyStates = Json.decodeFromJsonElement<Map<Int, JsonObject>>(element)
            for ([blockId, blockStates] in legacyStates.values.groupBy { it.getValue("block").jsonPrimitive.content }) {
                if (blockId in legacyVanillaBlocks)
                    continue
                
                val blockKey = Key.key(blockId)
                entries.getOrPut(Key.key("minecraft:block"), ::HashSet) += blockKey
                if (blockKey in states)
                    continue
                
                states[blockKey] = BlockConfiguration(
                    isTileEntity = true, // not known, so default to true
                    properties = LegacyBlockStateIdResolver.convertProperties(blockStates).values.toList()
                )
            }
        }
        
        files["custom_enchantment_ids"]?.let { element ->
            val ids = Json.decodeFromJsonElement(SetSerializer(KeySerializer), element)
            entries.getOrPut(Key.key("minecraft:enchantment"), ::HashSet) += ids
        }
        files["custom_entity_variant_keys"]?.let { element ->
            for (key in Json.decodeFromJsonElement(SetSerializer(ResourceKeySerializer), element)) {
                entries.getOrPut(key.registry().toKey(), ::HashSet) += key.identifier().toKey()
            }
        }
        
        queueStore("known_registry_entries", Json.encodeToJsonElement(entriesSerializer, entries))
        queueStore("known_block_states", Json.encodeToJsonElement(statesSerializer, states))
        remove("custom_enchantment_ids")
        remove("custom_entity_variant_keys")
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