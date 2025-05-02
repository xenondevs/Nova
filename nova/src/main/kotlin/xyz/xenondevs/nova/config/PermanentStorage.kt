package xyz.xenondevs.nova.config

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import net.kyori.adventure.key.Key
import xyz.xenondevs.commons.gson.fromJson
import xyz.xenondevs.commons.provider.MutableProvider
import xyz.xenondevs.commons.provider.mutableProvider
import xyz.xenondevs.commons.version.Version
import xyz.xenondevs.nova.PREVIOUS_NOVA_VERSION
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.serialization.json.GSON
import xyz.xenondevs.nova.serialization.kotlinx.KeySerializer
import xyz.xenondevs.nova.serialization.kotlinx.UUIDSerializer
import xyz.xenondevs.nova.serialization.kotlinx.VersionSerializer
import xyz.xenondevs.nova.util.data.readJson
import xyz.xenondevs.nova.util.data.writeJson
import xyz.xenondevs.nova.world.ChunkPos
import java.nio.file.Path
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists

@InternalInit(stage = InternalInitStage.PRE_WORLD)
internal object PermanentStorageMigrations {
    
    @InitFun
    private fun migrate() {
        if (PREVIOUS_NOVA_VERSION == null || PREVIOUS_NOVA_VERSION >= Version("0.19-alpha.3"))
            return
        
        // migrations for structured map keys from [[key, value], [key, value]] to [key, value, key, value]
        
        val blockChunkCounter = PermanentStorage.getPath("block_chunk_counter")
        if (blockChunkCounter.exists()) {
            val map = GSON.fromJson<HashMap<UUID, HashMap<ChunkPos, HashMap<Key, Int>>>>(blockChunkCounter)
            blockChunkCounter.writeJson(map, PermanentStorage.JSON)
        }
        
        val forceLoadedChunks = PermanentStorage.getPath("forceLoadedChunks")
        if (forceLoadedChunks.exists()) {
            val map = GSON.fromJson<HashMap<ChunkPos, HashSet<UUID>>>(forceLoadedChunks)
            forceLoadedChunks.writeJson(map, PermanentStorage.JSON)
        }
    }
    
}

internal object PermanentStorage {
    
    val JSON = Json {
        allowStructuredMapKeys = true
        serializersModule = SerializersModule {
            contextual(UUID::class, UUIDSerializer)
            contextual(Key::class, KeySerializer)
            contextual(Version::class, VersionSerializer)
        }
    }
    
    private val dir = Path("plugins/Nova/.internal_data/storage/")
    
    fun has(key: String): Boolean =
        getPath(key).exists()
    
    fun remove(key: String) =
        getPath(key).deleteIfExists()
    
    fun <T> store(key: String, serializer: SerializationStrategy<T>, data: T): Unit =
        getPath(key).writeJson(serializer, data, JSON)
    
    fun <T> retrieve(key: String, deserializer: DeserializationStrategy<T>): T? =
        getPath(key).takeIf { it.exists() }?.readJson(deserializer, JSON)
    
    inline fun <reified T> store(key: String, data: T): Unit =
        getPath(key).writeJson(data, JSON)
    
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