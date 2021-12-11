package xyz.xenondevs.nova.util.data

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.serialization.json.*
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.player.attachment.Attachment
import java.io.File
import java.lang.reflect.Type
import java.util.*
import kotlin.reflect.KProperty

val GSON: Gson =
    GsonBuilder()
        .setPrettyPrinting()
        .registerTypeHierarchyAdapter<UUID>(UUIDTypeAdapter)
        .registerTypeHierarchyAdapter<ItemStack>(ItemStackSerialization)
        .registerTypeHierarchyAdapter<Location>(LocationSerialization)
        .registerTypeHierarchyAdapter<Attachment>(AttachmentSerialization)
        .registerTypeHierarchyAdapter<World>(WorldTypeAdapter)
        .registerTypeHierarchyAdapter<NovaMaterial>(NovaMaterialSerialization)
        .registerTypeAdapter(EnumMap::class.java, EnumMapInstanceCreator)
        .enableComplexMapKeySerialization()
        .create()

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

fun JsonObject.getDouble(property: String) = if (hasNumber(property)) get(property).asDouble else null

fun JsonObject.getFloat(property: String) = if (hasNumber(property)) get(property).asFloat else null

fun JsonObject.getString(property: String, default: String): String = if (hasString(property)) get(property).asString else default

fun JsonObject.getNumber(property: String, default: Number): Number = if (hasNumber(property)) get(property).asNumber else default

fun JsonObject.getInt(property: String, default: Int) = if (hasNumber(property)) get(property).asInt else default

fun JsonObject.getDouble(property: String, default: Double) = if (hasNumber(property)) get(property).asDouble else default

fun JsonObject.getFloat(property: String, default: Float) = if (hasNumber(property)) get(property).asFloat else default

fun JsonObject.getBoolean(property: String, default: Boolean = false) = if (hasBoolean(property)) get(property).asBoolean else default

operator fun JsonObject.set(property: String, value: JsonElement) = add(property, value)

fun JsonElement.writeToFile(file: File) =
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

fun JsonArray.addAll(vararg elements: JsonElement) =
    elements.forEach(this::add)

fun JsonArray.addAll(doubleArray: DoubleArray) =
    doubleArray.forEach(this::add)

fun JsonArray.addAll(stringArray: Array<String>) =
    stringArray.forEach(this::add)

fun JsonArray.getAllStrings() =
    filter(JsonElement::isString).map { it.asString }

fun JsonArray.getAllDoubles() =
    filter(JsonElement::isNumber).map { it.asDouble }

fun <T> JsonArray.toStringList(consumer: (List<String>) -> T) =
    consumer(this.filter(JsonElement::isString).map(JsonElement::getAsString))

inline fun <reified T> Gson.fromJson(jsonElement: JsonElement?): T? {
    if (jsonElement == null) return null
    return fromJson(jsonElement, type<T>())
}

inline fun <reified T> GsonBuilder.registerTypeHierarchyAdapter(typeAdapter: Any): GsonBuilder =
    registerTypeHierarchyAdapter(T::class.java, typeAdapter)

inline fun <reified T> type(): Type = object : TypeToken<T>() {}.type

open class MemberAccessor<T>(
    private val jsonObject: JsonObject,
    private val memberName: String,
    private val toType: (JsonElement) -> T,
    private val fromType: (T) -> JsonElement
) {
    
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T? {
        val element = jsonObject.get(memberName)
        return if (element != null) toType(element) else null
    }
    
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) {
        if (value != null) {
            jsonObject.add(memberName, fromType(value))
        } else {
            jsonObject.remove(memberName)
        }
    }
    
}

class IntAccessor(jsonObject: JsonObject, memberName: String) :
    MemberAccessor<Int>(
        jsonObject,
        memberName,
        { it.asInt },
        { JsonPrimitive(it) }
    )
