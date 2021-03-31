package xyz.xenondevs.nova.util

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import java.io.File
import java.lang.reflect.Type
import kotlin.reflect.KProperty

val GSON: Gson = GsonBuilder()
    .setPrettyPrinting()
    .create()

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

inline fun <reified T> Gson.fromJson(jsonElement: JsonElement?): T? {
    if (jsonElement == null) return null
    return fromJson(jsonElement, type<T>())
}

inline fun <reified T> GsonBuilder.registerTypeAdapter(typeAdapter: TypeAdapter<T>): GsonBuilder =
    registerTypeAdapter(type<T>(), typeAdapter)

inline fun <reified T> GsonBuilder.registerTypeAdapter(typeAdapter: JsonSerializer<T>): GsonBuilder =
    registerTypeAdapter(type<T>(), typeAdapter)

inline fun <reified T> GsonBuilder.registerTypeAdapter(typeAdapter: JsonDeserializer<T>): GsonBuilder =
    registerTypeAdapter(type<T>(), typeAdapter)

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
