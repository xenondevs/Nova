package xyz.xenondevs.nova.data.serialization.json

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import org.bukkit.NamespacedKey

object NamespacedKeyTypeAdapter : TypeAdapter<NamespacedKey>() {
    
    override fun write(writer: JsonWriter, value: NamespacedKey) {
        writer.value(value.toString())
    }
    
    override fun read(reader: JsonReader): NamespacedKey {
        return NamespacedKey.fromString(reader.nextString())!!
    }
    
}