package xyz.xenondevs.nova.serialization.json.serializer

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import net.kyori.adventure.key.Key

internal object KeyTypeAdapter : TypeAdapter<Key>() {
    
    override fun write(writer: JsonWriter, value: Key) {
        writer.value(value.asString())
    }
    
    override fun read(reader: JsonReader): Key? {
        return Key.key(reader.nextString())
    }
    
}