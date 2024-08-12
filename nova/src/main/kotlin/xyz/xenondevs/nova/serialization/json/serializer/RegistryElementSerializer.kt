package xyz.xenondevs.nova.serialization.json.serializer

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import net.minecraft.core.Registry
import xyz.xenondevs.nova.util.get

internal class RegistryElementSerializer<T>(private val registry: Registry<T>) : TypeAdapter<T>() {
    
    override fun write(writer: JsonWriter, value: T?) {
        if (value != null) {
            writer.value(registry.getKey(value).toString())
        } else {
            writer.nullValue()
        }
    }
    
    override fun read(reader: JsonReader): T? {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull()
            return null
        }
        
        val id = reader.nextString()
        return registry[id]
    }
    
    
}