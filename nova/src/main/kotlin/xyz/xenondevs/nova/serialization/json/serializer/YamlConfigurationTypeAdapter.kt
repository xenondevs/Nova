package xyz.xenondevs.nova.serialization.json.serializer

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import org.bukkit.configuration.file.YamlConfiguration
import java.io.StringReader

internal object YamlConfigurationTypeAdapter : TypeAdapter<YamlConfiguration>() {
    
    override fun write(writer: JsonWriter, value: YamlConfiguration) {
        writer.value(value.saveToString())
    }
    
    override fun read(reader: JsonReader): YamlConfiguration {
        return YamlConfiguration.loadConfiguration(StringReader(reader.nextString()))
    }
    
}