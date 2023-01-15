package xyz.xenondevs.nova.util.data

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import com.google.gson.reflect.TypeToken
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.World
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.data.resources.builder.content.font.FontChar
import xyz.xenondevs.nova.data.resources.model.data.BlockModelData
import xyz.xenondevs.nova.data.serialization.json.BlockModelDataSerialization
import xyz.xenondevs.nova.data.serialization.json.EnumMapInstanceCreator
import xyz.xenondevs.nova.data.serialization.json.FontCharSerialization
import xyz.xenondevs.nova.data.serialization.json.IntRangeSerialization
import xyz.xenondevs.nova.data.serialization.json.ItemStackSerialization
import xyz.xenondevs.nova.data.serialization.json.LocationSerialization
import xyz.xenondevs.nova.data.serialization.json.LootItemSerialization
import xyz.xenondevs.nova.data.serialization.json.LootTableSerialization
import xyz.xenondevs.nova.data.serialization.json.NamespacedIdTypeAdapter
import xyz.xenondevs.nova.data.serialization.json.NamespacedKeyTypeAdapter
import xyz.xenondevs.nova.data.serialization.json.NovaMaterialSerialization
import xyz.xenondevs.nova.data.serialization.json.UUIDTypeAdapter
import xyz.xenondevs.nova.data.serialization.json.VersionSerialization
import xyz.xenondevs.nova.data.serialization.json.WorldTypeAdapter
import xyz.xenondevs.nova.data.serialization.json.YamlConfigurationTypeAdapter
import xyz.xenondevs.nova.material.ItemNovaMaterial
import xyz.xenondevs.nova.world.loot.LootItem
import xyz.xenondevs.nova.world.loot.LootTable
import java.io.File
import java.io.InputStream
import java.io.Reader
import java.lang.reflect.Type
import java.nio.file.Path
import java.util.*
import kotlin.io.path.reader
import kotlin.io.path.writeText

private val GSON_BUILDER = GsonBuilder()
    .registerTypeHierarchyAdapter<UUID>(UUIDTypeAdapter)
    .registerTypeHierarchyAdapter<NamespacedId>(NamespacedIdTypeAdapter)
    .registerTypeHierarchyAdapter<NamespacedKey>(NamespacedKeyTypeAdapter)
    .registerTypeHierarchyAdapter<ItemStack>(ItemStackSerialization)
    .registerTypeHierarchyAdapter<Location>(LocationSerialization)
    .registerTypeHierarchyAdapter<World>(WorldTypeAdapter)
    .registerTypeHierarchyAdapter<ItemNovaMaterial>(NovaMaterialSerialization)
    .registerTypeHierarchyAdapter<YamlConfiguration>(YamlConfigurationTypeAdapter)
    .registerTypeHierarchyAdapter<IntRange>(IntRangeSerialization)
    .registerTypeHierarchyAdapter<LootTable>(LootTableSerialization)
    .registerTypeHierarchyAdapter<LootItem>(LootItemSerialization)
    .registerTypeHierarchyAdapter<BlockModelData>(BlockModelDataSerialization)
    .registerTypeHierarchyAdapter<Version>(VersionSerialization)
    .registerTypeAdapter<FontChar>(FontCharSerialization)
    .registerTypeAdapter(EnumMap::class.java, EnumMapInstanceCreator)
    .enableComplexMapKeySerialization()

val GSON: Gson = GSON_BUILDER.create()

val PRETTY_GSON: Gson = GSON_BUILDER.setPrettyPrinting().create()

fun File.parseJson(): JsonElement {
    try {
        return reader().use(JsonParser::parseReader)
    } catch (t: Throwable) {
        throw JsonParseException("Could not parse json file: $this", t)
    }
}

fun Path.parseJson(): JsonElement {
    try {
        return reader().use(JsonParser::parseReader)
    } catch (t: Throwable) {
        throw JsonParseException("Could not parse json file: $this", t)
    }
}

fun InputStream.parseJson(): JsonElement =
    use { JsonParser.parseReader(it.reader()) }

fun JsonObject.hasString(property: String) =
    has(property) && this[property].isString()

fun JsonObject.hasNumber(property: String) =
    has(property) && this[property].isNumber()

fun JsonObject.hasBoolean(property: String) =
    has(property) && this[property].isBoolean()

fun JsonObject.hasObject(property: String) =
    has(property) && this[property] is JsonObject

fun JsonObject.hasArray(property: String) =
    has(property) && this[property] is JsonArray

fun JsonObject.getString(property: String) = if (hasString(property)) get(property).asString else null

fun JsonObject.getNumber(property: String) = if (hasNumber(property)) get(property).asNumber else null

fun JsonObject.getInt(property: String) = if (hasNumber(property)) get(property).asInt else null

fun JsonObject.getLong(property: String) = if (hasNumber(property)) get(property).asLong else null

fun JsonObject.getDouble(property: String) = if (hasNumber(property)) get(property).asDouble else null

fun JsonObject.getFloat(property: String) = if (hasNumber(property)) get(property).asFloat else null

fun JsonObject.getString(property: String, default: String): String = if (hasString(property)) get(property).asString else default

fun JsonObject.getNumber(property: String, default: Number): Number = if (hasNumber(property)) get(property).asNumber else default

fun JsonObject.getInt(property: String, default: Int) = if (hasNumber(property)) get(property).asInt else default

fun JsonObject.getDouble(property: String, default: Double) = if (hasNumber(property)) get(property).asDouble else default

fun JsonObject.getFloat(property: String, default: Float) = if (hasNumber(property)) get(property).asFloat else default

fun JsonObject.getBoolean(property: String, default: Boolean = false) = if (hasBoolean(property)) get(property).asBoolean else default

fun JsonObject.getOrNull(property: String) = if (has(property)) get(property) else null

inline fun <reified T : JsonElement> JsonObject.getOrPut(property: String, defaultValue: () -> T): T {
    var value = getOrNull(property)
    
    if (value !is T) {
        value = defaultValue()
        set(property, value)
    }
    
    return value
}

inline fun <reified T> JsonObject.getDeserialized(property: String) = GSON.fromJson<T>(get(property))

inline fun <reified T> JsonObject.getDeserialized(property: String, default: () -> T) = getDeserialized(property)
    ?: default()

operator fun JsonObject.set(property: String, value: JsonElement) = add(property, value)

fun JsonElement.writeToFile(file: File) =
    file.writeText(toString())

fun JsonElement.writeToFile(file: Path) =
    file.writeText(toString())

fun JsonElement.isString() =
    this is JsonPrimitive && isString

fun JsonElement.isBoolean() =
    this is JsonPrimitive && isBoolean

fun JsonElement.isNumber() =
    this is JsonPrimitive && isNumber

fun JsonArray.addAll(vararg numbers: Number) =
    numbers.forEach(this::add)

fun JsonArray.addAll(vararg booleans: Boolean) =
    booleans.forEach(this::add)

fun JsonArray.addAll(vararg chars: Char) =
    chars.forEach(this::add)

fun JsonArray.addAll(vararg strings: String) =
    strings.forEach(this::add)

fun JsonArray.addAll(vararg elements: JsonElement) =
    elements.forEach(this::add)

fun JsonArray.addAll(intArray: IntArray) =
    intArray.forEach(this::add)

fun JsonArray.addAll(longArray: LongArray) =
    longArray.forEach(this::add)

fun JsonArray.addAll(floatArray: FloatArray) =
    floatArray.forEach(this::add)

fun JsonArray.addAll(doubleArray: DoubleArray) =
    doubleArray.forEach(this::add)

@JvmName("addAllBooleanArray")
fun JsonArray.addAll(booleanArray: BooleanArray) =
    booleanArray.forEach(this::add)

@JvmName("addAllCharArray")
fun JsonArray.addAll(charArray: CharArray) =
    charArray.forEach(this::add)

@JvmName("addAllStringArray")
fun JsonArray.addAll(stringArray: Array<String>) =
    stringArray.forEach(this::add)

@JvmName("addAllElementsArray")
fun JsonArray.addAll(elementArray: Array<JsonElement>) =
    elementArray.forEach(this::add)

@JvmName("addAllNumbers")
fun JsonArray.addAll(numbers: Iterable<Number>) =
    numbers.forEach(this::add)

@JvmName("addAllBooleans")
fun JsonArray.addAll(booleans: Iterable<Boolean>) =
    booleans.forEach(this::add)

@JvmName("addAllChars")
fun JsonArray.addAll(chars: Iterable<Char>) =
    chars.forEach(this::add)

@JvmName("addAllStrings")
fun JsonArray.addAll(strings: Iterable<String>) =
    strings.forEach(this::add)

@JvmName("addAllElements")
fun JsonArray.addAll(elements: Iterable<JsonElement>) =
    elements.forEach(this::add)

fun JsonArray.getAllStrings() =
    filter(JsonElement::isString).map { it.asString }

fun <T : MutableCollection<String>> JsonArray.getAllStringsTo(destination: T) =
    filter(JsonElement::isString).mapTo(destination) { it.asString }

fun JsonArray.getAllDoubles() =
    filter(JsonElement::isNumber).map { it.asDouble }

fun JsonArray.getAllInts() =
    filter(JsonElement::isNumber).map { it.asInt }

fun JsonArray.getAllJsonObjects() =
    filterIsInstance<JsonObject>()

fun <T> JsonArray.toStringList(consumer: (List<String>) -> T) =
    consumer(this.filter(JsonElement::isString).map(JsonElement::getAsString))

fun JsonObject.addAll(other: JsonObject) {
    other.entrySet().forEach { (property, value) -> add(property, value) }
}

inline fun <reified T> Gson.fromJson(json: String?): T? {
    if (json == null) return null
    return fromJson(json, type<T>())
}

inline fun <reified T> Gson.fromJson(jsonElement: JsonElement?): T? {
    if (jsonElement == null) return null
    return fromJson(jsonElement, type<T>())
}

inline fun <reified T> Gson.fromJson(reader: Reader): T? {
    return fromJson(reader, type<T>())
}

inline fun <reified T> GsonBuilder.registerTypeAdapter(typeAdapter: Any): GsonBuilder =
    registerTypeAdapter(T::class.java, typeAdapter)

inline fun <reified T> GsonBuilder.registerTypeHierarchyAdapter(typeAdapter: Any): GsonBuilder =
    registerTypeHierarchyAdapter(T::class.java, typeAdapter)

inline fun <reified T> type(): Type = object : TypeToken<T>() {}.type