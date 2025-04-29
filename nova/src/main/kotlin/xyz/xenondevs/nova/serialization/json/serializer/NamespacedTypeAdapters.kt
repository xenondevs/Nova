@file:Suppress("DEPRECATION")

package xyz.xenondevs.nova.serialization.json.serializer

import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import net.kyori.adventure.key.Key
import net.minecraft.resources.ResourceLocation
import org.bukkit.NamespacedKey
import xyz.xenondevs.bytebase.util.representedClass
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.ResourceType
import java.lang.reflect.ParameterizedType
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.isSubclassOf

internal object NamespacedTypeAdapters : TypeAdapterFactory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : Any?> create(gson: Gson?, typeToken: TypeToken<T?>?): TypeAdapter<T?>? {
        return when (val representedClass = typeToken?.type?.representedClass?.kotlin) {
            ResourcePath::class -> when (val type = typeToken.type) {
                is ParameterizedType -> {
                    val rType = (type.actualTypeArguments[0] as Class<*>).kotlin
                        .let { it.objectInstance ?: it.companionObjectInstance } as ResourceType
                    ResourcePathTypeAdapter(rType).nullSafe()
                }
                
                else -> GenericKeyTypeAdapter
            }
            
            
            NamespacedKey::class -> NamespacedKeyTypeAdapter
            ResourceLocation::class -> ResourceLocationTypeAdapter
            else -> if (representedClass?.isSubclassOf(Key::class) == true) GenericKeyTypeAdapter else null
       
        } as TypeAdapter<T?>?
    }
    
    private class ResourcePathTypeAdapter<T : ResourceType>(private val type: T) : TypeAdapter<ResourcePath<T>>() {
        
        override fun write(writer: JsonWriter, value: ResourcePath<T>) {
            writer.value(value.toString())
        }
        
        override fun read(reader: JsonReader): ResourcePath<T> {
            return ResourcePath.of(type, reader.nextString())
        }
        
    }
    
    private object NamespacedKeyTypeAdapter : TypeAdapter<NamespacedKey>() {
        
        override fun write(writer: JsonWriter, value: NamespacedKey) {
            writer.value(value.toString())
        }
        
        override fun read(reader: JsonReader): NamespacedKey {
            val str = reader.nextString()
            return NamespacedKey.fromString(str) ?: throw IllegalArgumentException("Invalid namespaced key: $str")
        }
        
    }
    
    private object GenericKeyTypeAdapter : TypeAdapter<Key>() {
        
        override fun write(writer: JsonWriter, value: Key) {
            writer.value(value.toString())
        }
        
        override fun read(reader: JsonReader): Key {
            return Key.key(reader.nextString())
        }
        
    }
    
    private object ResourceLocationTypeAdapter : TypeAdapter<ResourceLocation>() {
        
        override fun write(writer: JsonWriter, value: ResourceLocation) {
            writer.value(value.toString())
        }
        
        override fun read(reader: JsonReader): ResourceLocation {
            return ResourceLocation.parse(reader.nextString())
        }
        
    }
    
}