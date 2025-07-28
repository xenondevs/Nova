package xyz.xenondevs.nova.resources.builder.data

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import xyz.xenondevs.downloader.ExtractionMode
import xyz.xenondevs.downloader.MinecraftAssetsDownloader
import xyz.xenondevs.nova.resources.builder.model.Model
import java.nio.file.FileSystem
import java.nio.file.Path
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText
import kotlin.io.path.relativeTo
import kotlin.io.path.walk

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
        // weak round trip because many vanilla models have "__comment" keys or redundant properties
        weakRoundTrip<Model>("assets/minecraft/models/")
    }
    
    // TODO: round trip test item model definitions
    
    private inline fun <reified T> roundTrip(dir: String) {
        val base = assets.resolve(dir)
        base.walk()
            .filter { it.isRegularFile() }
            .forEach { file ->
                try {
                    val content = file.readText()
                    assertJsonEquals(
                        Json.parseToJsonElement(content),
                        Json.encodeToJsonElement(Json.decodeFromString<T>(content)),
                        file.relativeTo(base).invariantSeparatorsPathString
                    )
                } catch (e: Exception) {
                    throw AssertionError("Failed to de(serialize) file: ${file.relativeTo(base)}", e)
                }
            }
    }
    
    private inline fun <reified T> weakRoundTrip(dir: String) {
        val json = Json { ignoreUnknownKeys = true }
        val base = assets.resolve(dir)
        base.walk()
            .filter { it.isRegularFile() }
            .forEach { file ->
                try {
                    val content = file.readText()
                    assertJsonContainsAll(
                        json.parseToJsonElement(content),
                        json.encodeToJsonElement(json.decodeFromString<T>(content)),
                        file.relativeTo(base).invariantSeparatorsPathString
                    )
                } catch (e: Exception) {
                    throw AssertionError("Failed to de(serialize) file: ${file.relativeTo(base)}", e)
                }
            }
    }
    
    private fun JsonPrimitive.sanitizedEquals(other: JsonPrimitive): Boolean {
        // remove "minecraft:" namespace prefix
        if (isString)
            return content.removePrefix("minecraft:") == other.content.removePrefix("minecraft:")
        
        val d1 = content.toDoubleOrNull()
        val d2 = other.content.toDoubleOrNull()
        if (d1 != null)
            return d1 == d2
        
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
    
    private fun assertJsonContainsAll(superset: JsonElement, subset: JsonElement, message: String? = null) {
        if (!superset.sanitizedContainsAll(subset)) {
            throw AssertionError(buildString {
                if (message != null) {
                    appendLine(message)
                    appendLine()
                }
                appendLine("JSON content does not match (formatting is ignored, 'minecraft:' prefixes were removed):")
                appendLine("Superset:")
                append(superset)
                appendLine()
                appendLine("Subset:")
                append(subset)
            })
        }
    }
    
    
}