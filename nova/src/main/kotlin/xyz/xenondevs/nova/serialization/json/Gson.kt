package xyz.xenondevs.nova.serialization.json

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import xyz.xenondevs.commons.gson.fromJson
import xyz.xenondevs.commons.gson.registerTypeAdapter
import xyz.xenondevs.commons.gson.registerTypeHierarchyAdapter
import xyz.xenondevs.commons.gson.toJsonTreeTyped
import xyz.xenondevs.nova.serialization.json.serializer.EnumMapInstanceCreator
import xyz.xenondevs.nova.serialization.json.serializer.IntRangeSerialization
import xyz.xenondevs.nova.serialization.json.serializer.LootItemSerialization
import xyz.xenondevs.nova.serialization.json.serializer.LootTableSerialization
import xyz.xenondevs.nova.serialization.json.serializer.NamespacedTypeAdapters
import xyz.xenondevs.nova.serialization.json.serializer.SizeOverrideSerialization
import xyz.xenondevs.nova.serialization.json.serializer.UUIDTypeAdapter

private val GSON_BUILDER = GsonBuilder()
    .disableHtmlEscaping()
    .enableComplexMapKeySerialization()
    .registerTypeAdapterFactory(NamespacedTypeAdapters)
    .registerTypeHierarchyAdapter(IntRangeSerialization)
    .registerTypeHierarchyAdapter(LootTableSerialization)
    .registerTypeHierarchyAdapter(LootItemSerialization)
    .registerTypeAdapter(UUIDTypeAdapter.nullSafe())
    .registerTypeAdapter(SizeOverrideSerialization)
    .registerTypeAdapter(EnumMapInstanceCreator)

internal val GSON: Gson = GSON_BUILDER.create()

internal inline fun <reified T> JsonObject.getDeserializedOrNull(key: String): T? =
    GSON.fromJson<T>(get(key))

internal inline fun <reified T> JsonObject.getDeserialized(key: String): T {
    if (!has(key))
        throw NoSuchElementException("No JsonElement with key $key found.")
    
    return GSON.fromJson<T>(get(key))
        ?: throw NullPointerException("Could not deserialize JsonElement with key $key.")
}

internal inline fun <reified T> JsonObject.addSerialized(key: String, value: T) =
    add(key, GSON.toJsonTreeTyped(value))