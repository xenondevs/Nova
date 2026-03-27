package xyz.xenondevs.nova.resources.builder.data

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import xyz.xenondevs.downloader.ExtractionMode
import xyz.xenondevs.downloader.MinecraftAssetsDownloader
import xyz.xenondevs.nova.resources.builder.model.Model
import xyz.xenondevs.nova.serialization.kotlinx.Matrix4fcAsArraySerializer
import xyz.xenondevs.nova.serialization.kotlinx.Matrix4fcAsSingularValueDecompositionMultiFormatSerializer
import java.nio.file.FileSystem
import java.nio.file.Path
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText
import kotlin.io.path.relativeTo
import kotlin.io.path.walk
import kotlin.math.abs

class DtoSerializationTest {
    
    companion object {
        
        private lateinit var fs: FileSystem
        private lateinit var assets: Path
        
        @JvmStatic
        @BeforeAll
        fun setup() = runBlocking {
            fs = Jimfs.newFileSystem(Configuration.unix())
            assets = fs.rootDirectories.first()
            MinecraftAssetsDownloader(
                System.getenv("MINECRAFT_VERSION"), // defined in test task configuration
                assets,
                ExtractionMode.MOJANG_API_CLIENT,
                LoggerFactory.getLogger(DtoSerializationTest::class.java)
            ).downloadAssets()
        }
        
        @JvmStatic
        @AfterAll
        fun tearDown() {
            fs.close()
        }
        
    }
    
    @Test
    fun blockStatesRoundTrip() {
        roundTrip<BlockStateDefinition>("assets/minecraft/blockstates/")
    }
    
    @Test
    fun modelsRoundTrip() {
        roundTrip<Model>(
            "assets/minecraft/models/",
            discardKeys = setOf("__comment", "name", "groups", "texture_size"),
            conditionalRemovals = mapOf(
                "translation" to { it is JsonArray && it.size == 3 && it.all { e -> (e as? JsonPrimitive)?.contentOrNull?.toDoubleOrNull() == 0.0 } },
                "rotation" to { it is JsonArray && it.size == 3 && it.all { e -> (e as? JsonPrimitive)?.contentOrNull?.toDoubleOrNull() == 0.0 } },
                "scale" to { it is JsonArray && it.size == 3 && it.all { e -> (e as? JsonPrimitive)?.contentOrNull?.toDoubleOrNull() == 1.0 } },
                "rescale" to { it is JsonPrimitive && it.booleanOrNull == false }
            )
        )
    }
    
    @Test
    fun itemModelDefinitionsRoundTrip() {
        roundTrip<ItemModelDefinition>("assets/minecraft/items/",
            reserializations = listOf(
                Triple(
                    "transformation",
                    Matrix4fcAsSingularValueDecompositionMultiFormatSerializer,
                    Matrix4fcAsArraySerializer
                )
            )
        )
    }
    
    @Suppress("UNCHECKED_CAST")
    private inline fun <reified T> roundTrip(
        dir: String,
        
        // pre-processing content: removes keys from both expected JSON and input for actual round-trip
        discardKeys: Set<String> = emptySet(),
        
        // transforms expected JSON only (does not affect round-trip input data)
        conditionalRemovals: Map<String, (JsonElement) -> Boolean> = emptyMap(),
        reserializations: List<Triple<String, DeserializationStrategy<*>, SerializationStrategy<*>>> = emptyList()
    ) {
        val base = assets.resolve(dir)
        base.walk()
            .filter { it.isRegularFile() }
            .forEach { file ->
                val content = Json.parseToJsonElement(file.readText()).withoutKeys(discardKeys)
                try {
                    var expected = content
                    expected = expected.removeConditionally(conditionalRemovals)
                    expected = reserializations.fold(expected) { acc, (n, d, s) ->
                        d as DeserializationStrategy<Any>
                        s as SerializationStrategy<Any>
                        acc.reserializeNamed(n, d, s)
                    }
                    
                    val actual = Json.encodeToJsonElement(Json.decodeFromJsonElement<T>(content))
                    
                    assertJsonEquals(
                        expected,
                        actual,
                        file.relativeTo(base).invariantSeparatorsPathString
                    )
                } catch (e: Exception) {
                    throw AssertionError("Failed to (de)serialize file: ${file.relativeTo(base)}.  Content:\n$content", e)
                }
            }
    }
    
    private fun JsonElement.withoutKeys(discardKeys: Set<String>): JsonElement = when (this) {
        is JsonObject -> JsonObject(
            entries
                .filterNot { (key, _) -> key in discardKeys }
                .associate { (key, value) -> key to value.withoutKeys(discardKeys) }
        )
        
        is JsonArray -> JsonArray(map { it.withoutKeys(discardKeys) })
        
        else -> this
    }
    
    private fun JsonElement.removeConditionally(conditions: Map<String, (JsonElement) -> Boolean>): JsonElement = when (this) {
        is JsonObject -> JsonObject(
            entries
                .filterNot { (k, v) -> k in conditions && conditions[k]!!.invoke(v) }
                .associate { (k, v) -> k to v.removeConditionally(conditions) }
        )
        
        is JsonArray -> JsonArray(map { it.removeConditionally(conditions) })
        
        else -> this
    }
    
    private fun <T> JsonElement.reserializeNamed(
        name: String,
        deserializer: DeserializationStrategy<T>,
        serializer: SerializationStrategy<T>
    ): JsonElement = when (this) {
        is JsonObject -> JsonObject(entries.associate { (key, value) ->
            val newValue = if (key == name)
                value.reserialize(deserializer, serializer)
            else value.reserializeNamed(name, deserializer, serializer)
            key to newValue
        })
        
        is JsonArray -> JsonArray(map { it.reserializeNamed(name, deserializer, serializer) })
        
        else -> this
    }
    
    private fun <T> JsonElement.reserialize(
        deserializer: DeserializationStrategy<T>,
        serializer: SerializationStrategy<T>
    ): JsonElement = Json.encodeToJsonElement(serializer, Json.decodeFromJsonElement(deserializer, this))
    
    private fun JsonPrimitive.sanitizedEquals(other: JsonPrimitive): Boolean {
        // remove "minecraft:" namespace prefix
        if (isString)
            return content.removePrefix("minecraft:") == other.content.removePrefix("minecraft:")
        
        val d1 = content.toDoubleOrNull()
        val d2 = other.content.toDoubleOrNull()
        if (d1 != null && d2 != null)
            return abs(d1 - d2) < 1e-6
        
        return this == other
    }
    
    private fun JsonElement.sanitizedEquals(other: JsonElement): Boolean {
        return when (this) {
            is JsonObject if other is JsonObject -> this.all { (key, value) -> other[key]?.sanitizedEquals(value) ?: false }
            is JsonArray if other is JsonArray -> this.size == other.size && this.withIndex().all { (i, value) -> value.sanitizedEquals(other.getOrNull(i) ?: return false) }
            is JsonPrimitive if other is JsonPrimitive -> this.sanitizedEquals(other)
            else -> this == other
        }
    }
    
    private fun JsonElement.sanitizedContainsAll(other: JsonElement): Boolean {
        return when (this) {
            is JsonObject if other is JsonObject -> other.all { (key, value) -> this[key]?.sanitizedContainsAll(value) ?: false }
            is JsonArray if other is JsonArray -> other.withIndex().all { (i, value) -> this.getOrNull(i)?.sanitizedContainsAll(value) ?: false }
            is JsonPrimitive if other is JsonPrimitive -> this.sanitizedEquals(other)
            else -> this == other
        }
    }
    
    private fun assertJsonEquals(expected: JsonElement, actual: JsonElement, message: String? = null) {
        if (!expected.sanitizedEquals(actual)) {
            throw AssertionError(buildString {
                if (message != null) {
                    appendLine(message)
                    appendLine()
                }
                appendLine("JSON content does not match (formatting is ignored, 'minecraft:' prefixes were removed):")
                appendLine("Expected:")
                append(expected)
                appendLine()
                appendLine("Actual:")
                append(actual)
            })
        }
    }
    
}