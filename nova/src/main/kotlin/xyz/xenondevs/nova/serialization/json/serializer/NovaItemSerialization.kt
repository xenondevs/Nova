package xyz.xenondevs.nova.serialization.json.serializer

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import xyz.xenondevs.nova.world.item.NovaItem
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.util.get

internal object NovaItemSerialization : TypeAdapter<NovaItem>() {
    
    override fun write(writer: JsonWriter, value: NovaItem?) {
        if (value != null) {
            writer.value(value.id.toString())
        } else {
            writer.nullValue()
        }
    }
    
    override fun read(reader: JsonReader): NovaItem? {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull()
            return null
        }
        
        val id = reader.nextString()
        return NovaRegistries.ITEM[id]
    }
    
}