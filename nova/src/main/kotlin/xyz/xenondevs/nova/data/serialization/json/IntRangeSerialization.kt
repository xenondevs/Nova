package xyz.xenondevs.nova.data.serialization.json

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import xyz.xenondevs.nova.util.data.isNumber
import java.lang.reflect.Type

internal object IntRangeSerialization : JsonSerializer<IntRange>, JsonDeserializer<IntRange> {
    
    override fun serialize(src: IntRange, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        if (src.first == src.last) return JsonPrimitive(src.first)
        val obj = JsonObject()
        obj.addProperty("min", src.first)
        obj.addProperty("max", src.last)
        return obj
    }
    
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): IntRange {
        if (json.isNumber()) return IntRange(json.asInt, json.asInt)
        val obj = json.asJsonObject
        val min = obj.get("min").asInt
        val max = obj.get("max").asInt
        return IntRange(min, max)
    }
    
}