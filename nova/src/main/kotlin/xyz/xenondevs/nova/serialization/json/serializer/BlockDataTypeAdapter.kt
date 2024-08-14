package xyz.xenondevs.nova.serialization.json.serializer

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import org.bukkit.Bukkit
import org.bukkit.block.data.BlockData

internal object BlockDataTypeAdapter : TypeAdapter<BlockData>() {
    
    override fun write(writer: JsonWriter, value: BlockData) {
        writer.value(value.asString)
    }
    
    override fun read(reader: JsonReader): BlockData {
        val serialized = reader.nextString()
        return Bukkit.getServer().createBlockData(serialized)
    }
    
}