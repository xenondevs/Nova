package xyz.xenondevs.nova.tileentity.serialization

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.util.*

object UUIDTypeAdapter : TypeAdapter<UUID>() {
    
    override fun write(writer: JsonWriter, value: UUID) {
        writer.value(value.toString())
    }
    
    override fun read(reader: JsonReader): UUID {
        return UUID.fromString(reader.nextString())
    }
    
}