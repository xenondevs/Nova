package xyz.xenondevs.nova.serialization.json.serializer

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import xyz.xenondevs.commons.version.Version

internal object VersionSerialization : TypeAdapter<Version>() {
    
    override fun write(writer: JsonWriter, value: Version) {
        writer.value(value.toString())
    }
    
    override fun read(reader: JsonReader): Version {
        return Version(reader.nextString())
    }
    
}