package xyz.xenondevs.nova.data.serialization.json

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import xyz.xenondevs.commons.gson.fromJson
import xyz.xenondevs.commons.gson.registerTypeAdapter
import xyz.xenondevs.commons.gson.registerTypeHierarchyAdapter
import xyz.xenondevs.nova.data.serialization.json.serializer.BlockModelDataSerialization
import xyz.xenondevs.nova.data.serialization.json.serializer.EnumMapInstanceCreator
import xyz.xenondevs.nova.data.serialization.json.serializer.FontCharSerialization
import xyz.xenondevs.nova.data.serialization.json.serializer.IntRangeSerialization
import xyz.xenondevs.nova.data.serialization.json.serializer.ItemStackSerialization
import xyz.xenondevs.nova.data.serialization.json.serializer.LocationSerialization
import xyz.xenondevs.nova.data.serialization.json.serializer.LootItemSerialization
import xyz.xenondevs.nova.data.serialization.json.serializer.LootTableSerialization
import xyz.xenondevs.nova.data.serialization.json.serializer.NamespacedIdTypeAdapter
import xyz.xenondevs.nova.data.serialization.json.serializer.NamespacedKeyTypeAdapter
import xyz.xenondevs.nova.data.serialization.json.serializer.ResourceLocationTypeAdapter
import xyz.xenondevs.nova.data.serialization.json.serializer.ResourcePathTypeAdapter
import xyz.xenondevs.nova.data.serialization.json.serializer.UUIDTypeAdapter
import xyz.xenondevs.nova.data.serialization.json.serializer.VersionSerialization
import xyz.xenondevs.nova.data.serialization.json.serializer.WorldTypeAdapter
import xyz.xenondevs.nova.data.serialization.json.serializer.YamlConfigurationTypeAdapter
import java.util.*

private val GSON_BUILDER = GsonBuilder()
    .registerTypeHierarchyAdapter(UUIDTypeAdapter)
    .registerTypeHierarchyAdapter(NamespacedIdTypeAdapter)
    .registerTypeHierarchyAdapter(NamespacedKeyTypeAdapter)
    .registerTypeHierarchyAdapter(ResourceLocationTypeAdapter)
    .registerTypeHierarchyAdapter(ResourcePathTypeAdapter)
    .registerTypeHierarchyAdapter(ItemStackSerialization)
    .registerTypeHierarchyAdapter(LocationSerialization)
    .registerTypeHierarchyAdapter(WorldTypeAdapter)
    .registerTypeHierarchyAdapter(YamlConfigurationTypeAdapter)
    .registerTypeHierarchyAdapter(IntRangeSerialization)
    .registerTypeHierarchyAdapter(LootTableSerialization)
    .registerTypeHierarchyAdapter(LootItemSerialization)
    .registerTypeHierarchyAdapter(BlockModelDataSerialization)
    .registerTypeHierarchyAdapter(VersionSerialization)
    .registerTypeAdapter(FontCharSerialization)
    .registerTypeAdapter(EnumMap::class.java, EnumMapInstanceCreator)
    .enableComplexMapKeySerialization()

val GSON: Gson = GSON_BUILDER.create()
val PRETTY_GSON: Gson = GSON_BUILDER.setPrettyPrinting().create()

inline fun <reified T> JsonObject.getDeserializedOrNull(key: String): T? =
    GSON.fromJson<T>(get(key))

inline fun <reified T> JsonObject.getDeserialized(key: String): T {
    if (!has(key))
        throw NoSuchElementException("No JsonElement with key $key found.")
    
    return GSON.fromJson<T>(get(key))
        ?: throw NullPointerException("Could not deserialize JsonElement with key $key.")
}