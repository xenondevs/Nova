package xyz.xenondevs.nova.data.serialization.json

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import org.bukkit.Bukkit
import org.bukkit.World

internal object WorldTypeAdapter : TypeAdapter<World?>() {
    
    override fun write(writer: JsonWriter, src: World?) {
        if (src != null)
            writer.value(src.name)
        else writer.nullValue()
    }
    
    override fun read(reader: JsonReader): World? {
        if (reader.peek() == JsonToken.NULL)
            return null
        
        return Bukkit.getWorld(reader.nextString())!!
    }
    
}