package xyz.xenondevs.nova.serialization.json.serializer

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import net.minecraft.resources.ResourceLocation

internal object ResourceLocationTypeAdapter : TypeAdapter<ResourceLocation?>() {
    
    override fun write(writer: JsonWriter, value: ResourceLocation?) {
        if (value != null)
            writer.value(value.toString())
        else writer.nullValue()
    }
    
    override fun read(reader: JsonReader): ResourceLocation? {
        if (reader.peek() == JsonToken.NULL)
            return null
        
        return ResourceLocation.parse(reader.nextString())
    }
    
}