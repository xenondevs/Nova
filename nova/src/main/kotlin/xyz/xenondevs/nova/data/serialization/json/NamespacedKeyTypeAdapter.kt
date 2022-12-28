package xyz.xenondevs.nova.data.serialization.json

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import org.bukkit.NamespacedKey

internal object NamespacedKeyTypeAdapter : TypeAdapter<NamespacedKey?>() {
    
    override fun write(writer: JsonWriter, value: NamespacedKey?) {
        if (value != null)
            writer.value(value.toString())
        else writer.nullValue()
    }
    
    override fun read(reader: JsonReader): NamespacedKey? {
        if (reader.peek() == JsonToken.NULL)
            return null
        
        return NamespacedKey.fromString(reader.nextString())!!
    }
    
}