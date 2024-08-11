package xyz.xenondevs.nova.serialization.json

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import xyz.xenondevs.commons.gson.fromJson
import xyz.xenondevs.commons.gson.registerTypeAdapter
import xyz.xenondevs.commons.gson.registerTypeHierarchyAdapter
import xyz.xenondevs.commons.gson.toJsonTreeTyped
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.serialization.json.serializer.BackingStateConfigSerialization
import xyz.xenondevs.nova.serialization.json.serializer.BlockDataTypeAdapter
import xyz.xenondevs.nova.serialization.json.serializer.BlockStateVariantDataSerialization
import xyz.xenondevs.nova.serialization.json.serializer.EnumMapInstanceCreator
import xyz.xenondevs.nova.serialization.json.serializer.FontCharSerialization
import xyz.xenondevs.nova.serialization.json.serializer.IntRangeSerialization
import xyz.xenondevs.nova.serialization.json.serializer.ItemStackSerialization
import xyz.xenondevs.nova.serialization.json.serializer.LinkedBlockModelProviderSerialization
import xyz.xenondevs.nova.serialization.json.serializer.LocationSerialization
import xyz.xenondevs.nova.serialization.json.serializer.LootItemSerialization
import xyz.xenondevs.nova.serialization.json.serializer.LootTableSerialization
import xyz.xenondevs.nova.serialization.json.serializer.Matrix2dcInstanceCreator
import xyz.xenondevs.nova.serialization.json.serializer.Matrix2fcInstanceCreator
import xyz.xenondevs.nova.serialization.json.serializer.Matrix3dcInstanceCreator
import xyz.xenondevs.nova.serialization.json.serializer.Matrix3fcInstanceCreator
import xyz.xenondevs.nova.serialization.json.serializer.Matrix3x2dcInstanceCreator
import xyz.xenondevs.nova.serialization.json.serializer.Matrix3x2fcInstanceCreator
import xyz.xenondevs.nova.serialization.json.serializer.Matrix4dcInstanceCreator
import xyz.xenondevs.nova.serialization.json.serializer.Matrix4fcInstanceCreator
import xyz.xenondevs.nova.serialization.json.serializer.Matrix4x3dcInstanceCreator
import xyz.xenondevs.nova.serialization.json.serializer.Matrix4x3fcInstanceCreator
import xyz.xenondevs.nova.serialization.json.serializer.ModelTypeAdapter
import xyz.xenondevs.nova.serialization.json.serializer.NamespacedIdTypeAdapter
import xyz.xenondevs.nova.serialization.json.serializer.NamespacedKeyTypeAdapter
import xyz.xenondevs.nova.serialization.json.serializer.NovaBlockStateSerialization
import xyz.xenondevs.nova.serialization.json.serializer.QuaterniondcInstanceCreator
import xyz.xenondevs.nova.serialization.json.serializer.QuaternionfcInstanceCreator
import xyz.xenondevs.nova.serialization.json.serializer.RegistryElementSerializer
import xyz.xenondevs.nova.serialization.json.serializer.ResourceLocationTypeAdapter
import xyz.xenondevs.nova.serialization.json.serializer.ResourcePathTypeAdapter
import xyz.xenondevs.nova.serialization.json.serializer.SizeOverrideSerialization
import xyz.xenondevs.nova.serialization.json.serializer.UUIDTypeAdapter
import xyz.xenondevs.nova.serialization.json.serializer.Vector2dcInstanceCreator
import xyz.xenondevs.nova.serialization.json.serializer.Vector2fcInstanceCreator
import xyz.xenondevs.nova.serialization.json.serializer.Vector2icInstanceCreator
import xyz.xenondevs.nova.serialization.json.serializer.Vector3dcInstanceCreator
import xyz.xenondevs.nova.serialization.json.serializer.Vector3fcInstanceCreator
import xyz.xenondevs.nova.serialization.json.serializer.Vector3icInstanceCreator
import xyz.xenondevs.nova.serialization.json.serializer.Vector4dcInstanceCreator
import xyz.xenondevs.nova.serialization.json.serializer.Vector4fcInstanceCreator
import xyz.xenondevs.nova.serialization.json.serializer.Vector4icInstanceCreator
import xyz.xenondevs.nova.serialization.json.serializer.VersionSerialization
import xyz.xenondevs.nova.serialization.json.serializer.WorldTypeAdapter
import xyz.xenondevs.nova.serialization.json.serializer.YamlConfigurationTypeAdapter

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
    .registerTypeHierarchyAdapter(RegistryElementSerializer(NovaRegistries.BLOCK))
    .registerTypeHierarchyAdapter(NovaBlockStateSerialization)
    .registerTypeHierarchyAdapter(BlockStateVariantDataSerialization)
    .registerTypeHierarchyAdapter(BackingStateConfigSerialization)
    .registerTypeHierarchyAdapter(LinkedBlockModelProviderSerialization)
    .registerTypeAdapter(RegistryElementSerializer(NovaRegistries.ITEM))
    .registerTypeAdapter(RegistryElementSerializer(NovaRegistries.GUI_TEXTURE))
    .registerTypeAdapter(SizeOverrideSerialization)
    .registerTypeAdapter(FontCharSerialization)
    .registerTypeAdapter(EnumMapInstanceCreator)
    .registerTypeAdapter(Matrix2dcInstanceCreator)
    .registerTypeAdapter(Matrix2fcInstanceCreator)
    .registerTypeAdapter(Matrix3dcInstanceCreator)
    .registerTypeAdapter(Matrix3fcInstanceCreator)
    .registerTypeAdapter(Matrix3x2dcInstanceCreator)
    .registerTypeAdapter(Matrix3x2fcInstanceCreator)
    .registerTypeAdapter(Matrix4dcInstanceCreator)
    .registerTypeAdapter(Matrix4fcInstanceCreator)
    .registerTypeAdapter(Matrix4x3dcInstanceCreator)
    .registerTypeAdapter(Matrix4x3fcInstanceCreator)
    .registerTypeAdapter(QuaterniondcInstanceCreator)
    .registerTypeAdapter(QuaternionfcInstanceCreator)
    .registerTypeAdapter(Vector2dcInstanceCreator)
    .registerTypeAdapter(Vector2fcInstanceCreator)
    .registerTypeAdapter(Vector2icInstanceCreator)
    .registerTypeAdapter(Vector3dcInstanceCreator)
    .registerTypeAdapter(Vector3fcInstanceCreator)
    .registerTypeAdapter(Vector3icInstanceCreator)
    .registerTypeAdapter(Vector4dcInstanceCreator)
    .registerTypeAdapter(Vector4fcInstanceCreator)
    .registerTypeAdapter(Vector4icInstanceCreator)

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