package xyz.xenondevs.nova.data.serialization.json

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import xyz.xenondevs.nova.util.data.Version

object VersionSerialization : TypeAdapter<Version>() {
    
    override fun write(writer: JsonWriter, value: Version) {
        writer.value(value.toString())
    }
    
    override fun read(reader: JsonReader): Version {
        return Version(reader.nextString())
    }
    
    
}