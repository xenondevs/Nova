package xyz.xenondevs.nova.serialization.json.serializer

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import net.minecraft.resources.ResourceLocation

internal object ResourceLocationTypeAdapter : TypeAdapter<ResourceLocation>() {
    
    override fun write(writer: JsonWriter, value: ResourceLocation) {
        writer.value(value.toString())
    }
    
    override fun read(reader: JsonReader): ResourceLocation {
        return ResourceLocation.parse(reader.nextString())
    }
    
}