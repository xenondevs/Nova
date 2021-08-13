package xyz.xenondevs.nova.data.serialization.gson

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import org.bukkit.Bukkit
import org.bukkit.World

object WorldTypeAdapter : TypeAdapter<World>() {
    
    override fun write(writer: JsonWriter, src: World) {
        writer.value(src.name)
    }
    
    override fun read(reader: JsonReader): World {
        return Bukkit.getWorld(reader.nextString())!!
    }
    
}