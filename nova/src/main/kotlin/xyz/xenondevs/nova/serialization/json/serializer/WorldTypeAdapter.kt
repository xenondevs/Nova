package xyz.xenondevs.nova.serialization.json.serializer

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import org.bukkit.Bukkit
import org.bukkit.World

internal object WorldTypeAdapter : TypeAdapter<World>() {
    
    override fun write(writer: JsonWriter, src: World) {
        writer.nullValue()
    }
    
    override fun read(reader: JsonReader): World {
        return Bukkit.getWorld(reader.nextString())!!
    }
    
}