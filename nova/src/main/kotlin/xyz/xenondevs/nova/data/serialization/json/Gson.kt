package xyz.xenondevs.nova.data.serialization.json

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import xyz.xenondevs.commons.gson.fromJson
import xyz.xenondevs.commons.gson.registerTypeAdapter
import xyz.xenondevs.commons.gson.registerTypeHierarchyAdapter
import xyz.xenondevs.commons.gson.toJsonTreeTyped
import xyz.xenondevs.nova.data.serialization.json.serializer.BackingStateConfigSerialization
import xyz.xenondevs.nova.data.serialization.json.serializer.BlockDataTypeAdapter
import xyz.xenondevs.nova.data.serialization.json.serializer.BlockStateVariantDataSerialization
import xyz.xenondevs.nova.data.serialization.json.serializer.EnumMapInstanceCreator
import xyz.xenondevs.nova.data.serialization.json.serializer.FontCharSerialization
import xyz.xenondevs.nova.data.serialization.json.serializer.IntRangeSerialization
import xyz.xenondevs.nova.data.serialization.json.serializer.ItemStackSerialization
import xyz.xenondevs.nova.data.serialization.json.serializer.LinkedBlockModelProviderSerialization
import xyz.xenondevs.nova.data.serialization.json.serializer.LocationSerialization
import xyz.xenondevs.nova.data.serialization.json.serializer.LootItemSerialization
import xyz.xenondevs.nova.data.serialization.json.serializer.LootTableSerialization
import xyz.xenondevs.nova.data.serialization.json.serializer.ModelTypeAdapter
import xyz.xenondevs.nova.data.serialization.json.serializer.NamespacedIdTypeAdapter
import xyz.xenondevs.nova.data.serialization.json.serializer.NamespacedKeyTypeAdapter
import xyz.xenondevs.nova.data.serialization.json.serializer.NovaBlockSerialization
import xyz.xenondevs.nova.data.serialization.json.serializer.NovaBlockStateSerialization
import xyz.xenondevs.nova.data.serialization.json.serializer.NovaItemSerialization
import xyz.xenondevs.nova.data.serialization.json.serializer.ResourceLocationTypeAdapter
import xyz.xenondevs.nova.data.serialization.json.serializer.ResourcePathTypeAdapter
import xyz.xenondevs.nova.data.serialization.json.serializer.SizeOverrideSerialization
import xyz.xenondevs.nova.data.serialization.json.serializer.UUIDTypeAdapter
import xyz.xenondevs.nova.data.serialization.json.serializer.VersionSerialization
import xyz.xenondevs.nova.data.serialization.json.serializer.WorldTypeAdapter
import xyz.xenondevs.nova.data.serialization.json.serializer.YamlConfigurationTypeAdapter
import java.util.*

private val GSON_BUILDER = GsonBuilder()
    .disableHtmlEscaping()
    .enableComplexMapKeySerialization()
    .registerTypeHierarchyAdapter(UUIDTypeAdapter)
    .registerTypeHierarchyAdapter(NamespacedIdTypeAdapter)
    .registerTypeHierarchyAdapter(NamespacedKeyTypeAdapter)
    .registerTypeHierarchyAdapter(ResourceLocationTypeAdapter)
    .registerTypeHierarchyAdapter(ResourcePathTypeAdapter)
    .registerTypeHierarchyAdapter(ItemStackSerialization)
    .registerTypeHierarchyAdapter(LocationSerialization)
    .registerTypeHierarchyAdapter(WorldTypeAdapter)
    .registerTypeHierarchyAdapter(BlockDataTypeAdapter)
    .registerTypeHierarchyAdapter(YamlConfigurationTypeAdapter)
    .registerTypeHierarchyAdapter(IntRangeSerialization)
    .registerTypeHierarchyAdapter(LootTableSerialization)
    .registerTypeHierarchyAdapter(LootItemSerialization)
    .registerTypeHierarchyAdapter(VersionSerialization)
    .registerTypeHierarchyAdapter(ModelTypeAdapter)
    .registerTypeHierarchyAdapter(NovaItemSerialization)
    .registerTypeHierarchyAdapter(NovaBlockSerialization)
    .registerTypeHierarchyAdapter(NovaBlockStateSerialization)
    .registerTypeHierarchyAdapter(BlockStateVariantDataSerialization)
    .registerTypeHierarchyAdapter(BackingStateConfigSerialization)
    .registerTypeHierarchyAdapter(LinkedBlockModelProviderSerialization)
    .registerTypeAdapter(SizeOverrideSerialization)
    .registerTypeAdapter(FontCharSerialization)
    .registerTypeAdapter(EnumMap::class.java, EnumMapInstanceCreator)

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

inline fun <reified T> JsonObject.addSerialized(key: String, value: T) =
    add(key, GSON.toJsonTreeTyped(value))