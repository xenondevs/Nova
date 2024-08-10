package xyz.xenondevs.nova.serialization.json.serializer

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import xyz.xenondevs.nova.resources.ResourcePath

internal object ResourcePathTypeAdapter : TypeAdapter<ResourcePath>() {
    
    override fun write(out: JsonWriter, value: ResourcePath) {
        out.value(value.toString())
    }
    
    override fun read(inr: JsonReader): ResourcePath {
        return ResourcePath.of(inr.nextString())
    }
    
}