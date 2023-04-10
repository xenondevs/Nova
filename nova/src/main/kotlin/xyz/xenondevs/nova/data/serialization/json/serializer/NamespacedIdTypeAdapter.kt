package xyz.xenondevs.nova.data.serialization.json.serializer

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import xyz.xenondevs.nova.data.NamespacedId

internal object NamespacedIdTypeAdapter : TypeAdapter<NamespacedId?>() {
    
    override fun write(writer: JsonWriter, value: NamespacedId?) {
        if (value != null)
            writer.value(value.toString())
        else writer.nullValue()
    }
    
    override fun read(reader: JsonReader): NamespacedId? {
        if (reader.peek() == JsonToken.NULL)
            return null
        
        return NamespacedId.of(reader.nextString())
    }
    
}