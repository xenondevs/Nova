package xyz.xenondevs.nova.serialization.json.serializer

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import org.bukkit.NamespacedKey

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