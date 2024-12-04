@file:Suppress("DEPRECATION")

package xyz.xenondevs.nova.serialization.json.serializer

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import net.kyori.adventure.key.Key
import net.minecraft.resources.ResourceLocation
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.api.NamespacedId
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.ResourceType
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

internal object NamespacedKeyTypeAdapter : TypeAdapter<NamespacedKey>() {
    
    override fun write(writer: JsonWriter, value: NamespacedKey) {
        writer.value(value.toString())
    }
    
    override fun read(reader: JsonReader): NamespacedKey {
        val str = reader.nextString()
        return NamespacedKey.fromString(str)
            ?: throw IllegalArgumentException("Invalid namespaced key: $str")
    }
    
}

internal object NamespacedIdTypeAdapter : TypeAdapter<NamespacedId>() {
    
    override fun write(writer: JsonWriter, value: NamespacedId) {
        writer.value(value.toString())
    }
    
    override fun read(reader: JsonReader): NamespacedId {
        return NamespacedId.of(reader.nextString())
    }
    
}

internal object ResourceLocationTypeAdapter : TypeAdapter<ResourceLocation>() {
    
    override fun write(writer: JsonWriter, value: ResourceLocation) {
        writer.value(value.toString())
    }
    
    override fun read(reader: JsonReader): ResourceLocation {
        return ResourceLocation.parse(reader.nextString())
    }
    
}

internal object ResourcePathSerialization : JsonSerializer<ResourcePath<*>>, JsonDeserializer<ResourcePath<*>> {
    
    override fun serialize(src: ResourcePath<*>, typeOfSrc: Type, context: JsonSerializationContext?): JsonElement {
        return JsonPrimitive(src.toString())
    }
    
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext?): ResourcePath<*> {
        val resourceType = ((typeOfT as ParameterizedType).actualTypeArguments[0] as Class<*>).kotlin.objectInstance as ResourceType
        return ResourcePath.of(resourceType, json.asString)
    }
    
}

internal object KeyTypeAdapter : TypeAdapter<Key>() {
    
    override fun write(writer: JsonWriter, value: Key) {
        writer.value(value.toString())
    }
    
    override fun read(reader: JsonReader): Key {
        return Key.key(reader.nextString())
    }
    
}