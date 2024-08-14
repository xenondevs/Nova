package xyz.xenondevs.nova.serialization.json.serializer

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import net.minecraft.core.Registry
import xyz.xenondevs.nova.util.get

internal class RegistryElementSerializer<T : Any>(private val registry: Registry<T>) : TypeAdapter<T>() {
    
    override fun write(writer: JsonWriter, value: T) {
        writer.value(registry.getKey(value).toString())
    }
    
    override fun read(reader: JsonReader): T {
        val id = reader.nextString()
        return registry[id] ?: throw IllegalArgumentException("Unknown registry element: $id")
    }
    
}