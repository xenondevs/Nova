@file:Suppress("DEPRECATION")

package xyz.xenondevs.nova.serialization.json.serializer

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import xyz.xenondevs.nova.api.NamespacedId

internal object NamespacedIdTypeAdapter : TypeAdapter<NamespacedId>() {
    
    override fun write(writer: JsonWriter, value: NamespacedId) {
        writer.value(value.toString())
    }
    
    override fun read(reader: JsonReader): NamespacedId {
        return NamespacedId.of(reader.nextString())
    }
    
}