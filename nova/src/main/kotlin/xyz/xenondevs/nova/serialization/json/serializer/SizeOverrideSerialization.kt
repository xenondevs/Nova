package xyz.xenondevs.nova.serialization.json.serializer

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import xyz.xenondevs.commons.gson.getInt
import xyz.xenondevs.commons.gson.getString
import xyz.xenondevs.nova.resources.builder.font.provider.unihex.SizeOverride
import java.lang.reflect.Type

internal object SizeOverrideSerialization : JsonSerializer<SizeOverride>, JsonDeserializer<SizeOverride> {
    
    override fun serialize(src: SizeOverride, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return JsonObject().apply {
            addProperty("from", Character.toString(src.from))
            addProperty("to", Character.toString(src.to))
            addProperty("left", src.left)
            addProperty("right", src.right)
        }
    }
    
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): SizeOverride {
        json as JsonObject
        return SizeOverride(
            json.getString("from").codePointAt(0),
            json.getString("to").codePointAt(0),
            json.getInt("left"),
            json.getInt("right"),
        )
    }
    
}