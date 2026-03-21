package xyz.xenondevs.nova.config

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.modules.SerializersModule
import net.kyori.adventure.key.Key
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ConfigStorageTest {
    
    private val testId = Key.key("test", "config")
    
    private fun jsonObj(vararg pairs: Pair<String, Any>): JsonObject =
        JsonObject(pairs.associate { (k, v) ->
            k to when (v) {
                is String -> JsonPrimitive(v)
                is Int -> JsonPrimitive(v)
                is Long -> JsonPrimitive(v)
                is Double -> JsonPrimitive(v)
                is Boolean -> JsonPrimitive(v)
                is JsonObject -> v
                else -> error("Unsupported type: ${v::class}")
            }
        })
    
    private fun nestedObj(vararg segments: String, value: Any): JsonObject {
        val leaf = jsonObj(segments.last() to value)
        return segments.dropLast(1).foldRight(leaf) { segment, acc -> jsonObj(segment to acc) }
    }
    
    //<editor-fold desc="ConfigStorage.get">
    
    @Test
    fun `get returns empty config for nonexistent id`() {
        val storage = ConfigStorage(
            SerializersModule { },
            MapBackend(emptyMap())
        )
        val provider = storage[testId]
        assertEquals(JsonObject(emptyMap()), provider.get())
    }
    
    @Test
    fun `get loads config from backend`() {
        val obj = jsonObj("key" to "value")
        val storage = ConfigStorage(
            SerializersModule { },
            MapBackend(mapOf(testId to obj))
        )
        val provider = storage[testId]
        assertEquals(obj, provider.get())
    }
    
    @Test
    fun `get with string id delegates to Key-based get`() {
        val obj = jsonObj("key" to "value")
        val storage = ConfigStorage(
            SerializersModule { },
            MapBackend(mapOf(testId to obj))
        )
        val provider = storage["test:config"]
        assertEquals(obj, provider.get())
    }
    
    @Test
    fun `get returns same provider for same id`() {
        val storage = ConfigStorage(
            SerializersModule { },
            MapBackend(emptyMap())
        )
        val a = storage[testId]
        val b = storage[testId]
        assertSame(a, b)
    }
    
    @Test
    fun `configId is set on provider`() {
        val storage = ConfigStorage(
            SerializersModule { },
            MapBackend(emptyMap())
        )
        assertEquals(testId, storage[testId].configId)
    }
    
    //</editor-fold>
    
    //<editor-fold desc="entry deserialization">
    
    @Test
    fun `entry reads string value`() {
        val obj = jsonObj("name" to "hello")
        val storage = ConfigStorage(SerializersModule { }, MapBackend(mapOf(testId to obj)))
        val provider = storage[testId]
        val entry = provider.entry<String>(default = "default", "name")
        assertEquals("hello", entry.get())
    }
    
    @Test
    fun `entry reads nested value`() {
        val obj = nestedObj("a", "b", "c", value = "deep")
        val storage = ConfigStorage(SerializersModule { }, MapBackend(mapOf(testId to obj)))
        val entry = storage[testId].entry<String>(default = "default", "a", "b", "c")
        assertEquals("deep", entry.get())
    }
    
    @Test
    fun `entry falls back to default for missing key`() {
        val storage = ConfigStorage(SerializersModule { }, MapBackend(mapOf(testId to jsonObj())))
        val entry = storage[testId].entry<String>(default = "fallback", "missing")
        assertEquals("fallback", entry.get())
    }
    
    @Test
    fun `entry falls back to default on type mismatch`() {
        val obj = jsonObj("key" to "not_an_int")
        val storage = ConfigStorage(SerializersModule { }, MapBackend(mapOf(testId to obj)))
        val entry = storage[testId].entry<Int>(99, "key")
        assertEquals(99, entry.get())
    }
    
    @Test
    fun `entry tries multiple paths and returns first match`() {
        val obj = jsonObj("new_name" to "found")
        val storage = ConfigStorage(SerializersModule { }, MapBackend(mapOf(testId to obj)))
        val entry = storage[testId].entry<String>(default = "default", listOf("old_name"), listOf("new_name"))
        assertEquals("found", entry.get())
    }
    
    @Test
    fun `optionalEntry returns null for missing key`() {
        val storage = ConfigStorage(SerializersModule { }, MapBackend(mapOf(testId to jsonObj())))
        val entry = storage[testId].optionalEntry<String>("missing")
        assertNull(entry.get())
    }
    
    @Test
    fun `optionalEntry returns value when present`() {
        val obj = jsonObj("key" to "value")
        val storage = ConfigStorage(SerializersModule { }, MapBackend(mapOf(testId to obj)))
        val entry = storage[testId].optionalEntry<String>("key")
        assertEquals("value", entry.get())
    }
    
    //</editor-fold>
    
    //<editor-fold desc="reload">
    
    @Test
    fun `reload updates modified configs`() {
        val backend = MutableMapBackend(mutableMapOf(testId to jsonObj("key" to "old")))
        val storage = ConfigStorage(SerializersModule { }, backend)
        
        val entry = storage[testId].entry<String>(default = "default", "key")
        assertEquals("old", entry.get())
        
        backend.configs[testId] = jsonObj("key" to "new")
        backend.modifiedTimes[testId] = System.currentTimeMillis() + 1000
        
        val reloaded = storage.reload()
        assertTrue(testId in reloaded)
        assertEquals("new", entry.get())
    }
    
    @Test
    fun `reload returns only modified config ids`() {
        val id1 = Key.key("test", "a")
        val id2 = Key.key("test", "b")
        val backend = MutableMapBackend(mutableMapOf(
            id1 to jsonObj("k" to "v1"),
            id2 to jsonObj("k" to "v2")
        ))
        val storage = ConfigStorage(SerializersModule { }, backend)
        storage[id1]
        storage[id2]
        
        // only modify id1
        backend.configs[id1] = jsonObj("k" to "updated")
        backend.modifiedTimes[id1] = System.currentTimeMillis() + 1000
        
        val reloaded = storage.reload()
        assertEquals(setOf(id1), reloaded)
    }
    
    @Test
    fun `reload skips config when backend returns null`() {
        val backend = MutableMapBackend(mutableMapOf(testId to jsonObj("key" to "old")))
        val storage = ConfigStorage(SerializersModule { }, backend)
        
        val entry = storage[testId].entry<String>(default = "default", "key")
        assertEquals("old", entry.get())
        
        backend.configs.remove(testId) // load will return null
        backend.modifiedTimes[testId] = System.currentTimeMillis() + 1000
        
        val reloaded = storage.reload()
        assertTrue(reloaded.isEmpty())
        assertEquals("old", entry.get()) // value preserved
    }
    
    //</editor-fold>
    
    //<editor-fold desc="error handling">
    
    @Test
    fun `onError called on deserialization failure`() {
        val errors = mutableListOf<Triple<Key, List<String>, SerializationException>>()
        val obj = jsonObj("key" to "not_an_int")
        val backend = MapBackend(mapOf(testId to obj), onError = { id, path, e -> errors += Triple(id, path, e) })
        val storage = ConfigStorage(SerializersModule { }, backend)
        
        storage[testId].optionalEntry<Int>("key").get()
        
        assertEquals(1, errors.size)
        assertEquals(testId, errors[0].first)
        assertEquals(listOf("key"), errors[0].second)
    }
    
    @Test
    fun `onError not called for missing keys`() {
        val errors = mutableListOf<Any>()
        val backend = MapBackend(mapOf(testId to jsonObj()), onError = { _, _, _ -> errors += Unit })
        val storage = ConfigStorage(SerializersModule { }, backend)
        
        storage[testId].optionalEntry<String>("missing").get()
        
        assertTrue(errors.isEmpty())
    }
    
    //</editor-fold>
    
    //<editor-fold desc="setSerializers">
    
    @Test
    fun `setSerializers scopes serializers to namespace`() {
        val customId = Key.key("custom", "config")
        val otherId = Key.key("other", "config")
        val obj = jsonObj("value" to "hello")
        val backend = MapBackend(mapOf(customId to obj, otherId to obj))
        val storage = ConfigStorage(SerializersModule { }, backend)
        
        storage.setSerializers("custom", SerializersModule {
            contextual(Wrapper::class, WrapperSerializer)
        })
        
        // "custom" namespace has the Wrapper serializer, so deserialization works
        val customEntry = storage[customId].optionalEntry<Wrapper>("value")
        assertEquals(Wrapper("hello"), customEntry.get())
        
        // "other" namespace doesn't, so it throws on deserialization
        val otherEntry = storage[otherId].optionalEntry<Wrapper>("value")
        assertThrows<SerializationException> { otherEntry.get() }
    }
    
    @Test
    fun `setSerializers rejects invalid namespace`() {
        val storage = ConfigStorage(SerializersModule { }, MapBackend(emptyMap()))
        assertThrows<IllegalArgumentException> {
            storage.setSerializers("INVALID!", SerializersModule { })
        }
    }
    
    //</editor-fold>
    
    //<editor-fold desc="test backends">
    
    private open class MapBackend(
        private val configs: Map<Key, JsonObject>,
        private val onError: (Key, List<String>, SerializationException) -> Unit = { _, _, _ -> }
    ) : ConfigBackend {
        override fun load(id: Key): JsonObject? = configs[id]
        override fun getLastModified(id: Key): Long = 0
        override fun onError(id: Key, path: List<String>, exception: SerializationException) =
            onError.invoke(id, path, exception)
    }
    
    private class MutableMapBackend(
        val configs: MutableMap<Key, JsonObject>,
        val modifiedTimes: MutableMap<Key, Long> = mutableMapOf()
    ) : ConfigBackend {
        override fun load(id: Key): JsonObject? = configs[id]
        override fun getLastModified(id: Key): Long = modifiedTimes[id] ?: 0
        override fun onError(id: Key, path: List<String>, exception: SerializationException) {}
    }
    
    //</editor-fold>
    
    //<editor-fold desc="test types">
    
    private data class Wrapper(val value: String)
    
    private object WrapperSerializer : KSerializer<Wrapper> {
        override val descriptor = PrimitiveSerialDescriptor("Wrapper", PrimitiveKind.STRING)
        override fun deserialize(decoder: Decoder) = Wrapper(decoder.decodeString())
        override fun serialize(encoder: Encoder, value: Wrapper) = encoder.encodeString(value.value)
    }
    
    //</editor-fold>
    
}
