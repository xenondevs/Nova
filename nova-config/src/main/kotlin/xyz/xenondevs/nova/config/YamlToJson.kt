package xyz.xenondevs.nova.config

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import org.snakeyaml.engine.v2.api.Dump
import org.snakeyaml.engine.v2.api.DumpSettings
import org.snakeyaml.engine.v2.api.Load
import org.snakeyaml.engine.v2.api.LoadSettings
import org.snakeyaml.engine.v2.api.StreamDataWriter
import org.snakeyaml.engine.v2.common.FlowStyle
import org.snakeyaml.engine.v2.common.ScalarStyle
import java.io.Reader

private val LOAD_SETTINGS = LoadSettings.builder()
    .setMaxAliasesForCollections(0) // prevent recursion
    .build()

private val DUMP_SETTINGS = DumpSettings.builder()
    .setDefaultFlowStyle(FlowStyle.BLOCK)
    .setDefaultScalarStyle(ScalarStyle.PLAIN)
    .build()

/**
 * Reads [yaml] with [settings] as a [JsonElement].
 * The given settings need to prevent recursion.
 */
fun readYamlAsJson(yaml: String, settings: LoadSettings = LOAD_SETTINGS): JsonElement =
    yamlToJson(Load(settings).loadFromString(yaml))

/**
 * Reads YAML from [reader] with [settings] as a [JsonElement].
 * The given settings need to prevent recursion.
 */
fun readYamlAsJson(reader: Reader, settings: LoadSettings = LOAD_SETTINGS): JsonElement =
    yamlToJson(Load(settings).loadFromReader(reader))

/**
 * Writes [json] as YAML with [settings] into a [String].
 */
fun writeJsonAsYaml(json: JsonElement, settings: DumpSettings = DUMP_SETTINGS): String =
    Dump(settings).dumpToString(jsonToJavaTree(json))

/**
 * Writes [json] as YAML with [settings] into [writer].
 */
fun writeJsonAsYaml(json: JsonElement, settings: DumpSettings = DUMP_SETTINGS, writer: StreamDataWriter) =
    Dump(settings).dump(jsonToJavaTree(json), writer)

private fun yamlToJson(
    value: Any?
): JsonElement = when (value) {
    null -> JsonNull
    is Boolean -> JsonPrimitive(value)
    is Number -> JsonPrimitive(value)
    is Char -> JsonPrimitive(value.toString())
    is String -> JsonPrimitive(value)
    is Iterable<*> -> JsonArray(value.map { yamlToJson(it) })
    is Map<*, *> -> JsonObject(value.entries.associate { (k, v) -> k.toString() to yamlToJson(v) })
    else -> throw IllegalArgumentException("Unexpected value: $value")
}

private fun jsonToJavaTree(
    element: JsonElement
): Any? = when (element) {
    is JsonNull -> null
    is JsonPrimitive -> {
        element.content.takeIf { element.isString }
            ?: element.longOrNull
            ?: element.doubleOrNull
            ?: element.booleanOrNull
    }
    
    is JsonArray -> element.map { jsonToJavaTree(it) }
    is JsonObject -> element.entries.associate { (k, v) -> k to jsonToJavaTree(v) }
}