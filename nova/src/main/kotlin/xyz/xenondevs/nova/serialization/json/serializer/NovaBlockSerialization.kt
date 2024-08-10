package xyz.xenondevs.nova.serialization.json.serializer

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.util.get
import xyz.xenondevs.nova.world.block.NovaBlock

internal object NovaBlockSerialization : TypeAdapter<NovaBlock>() {
    
    override fun write(writer: JsonWriter, value: NovaBlock?) {
        if (value != null) {
            writer.value(value.id.toString())
        } else {
            writer.nullValue()
        }
    }
    
    override fun read(reader: JsonReader): NovaBlock? {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull()
            return null
        }
        
        val id = reader.nextString()
        return NovaRegistries.BLOCK[id]
    }
    
}