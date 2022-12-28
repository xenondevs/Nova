package xyz.xenondevs.nova.data.serialization.json

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import xyz.xenondevs.nova.data.resources.builder.content.FontChar
import xyz.xenondevs.nova.util.data.getInt
import xyz.xenondevs.nova.util.data.getString
import java.lang.reflect.Type

internal object FontCharSerialization : JsonSerializer<FontChar>, JsonDeserializer<FontChar> {
    
    override fun serialize(src: FontChar, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return JsonObject().apply {
            addProperty("font", src.font)
            addProperty("char", src.char)
            addProperty("width", src.width)
        }
    }
    
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): FontChar {
        json as JsonObject
        return FontChar(
            json.getString("font")!!,
            json.getString("char")!!.first(),
            json.getInt("width")!!
        )
    }
    
}