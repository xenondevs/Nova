package xyz.xenondevs.nova.serialization.json.serializer

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import xyz.xenondevs.commons.gson.getIntOrNull
import xyz.xenondevs.commons.gson.getString
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.ResourceType
import xyz.xenondevs.nova.resources.builder.task.BlockStateVariantData
import java.lang.reflect.Type

internal object BlockStateVariantDataSerialization : JsonSerializer<BlockStateVariantData>, JsonDeserializer<BlockStateVariantData> {
    
    override fun serialize(src: BlockStateVariantData, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val obj = JsonObject()
        obj.addProperty("model", src.model.toString())
        if (src.x != 0)
            obj.addProperty("x", src.x)
        if (src.y != 0)
            obj.addProperty("y", src.y)
        return obj
    }
    
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): BlockStateVariantData {
        json as JsonObject
        return BlockStateVariantData(
            ResourcePath.of(ResourceType.Model, json.getString("model")),
            json.getIntOrNull("x") ?: 0,
            json.getIntOrNull("y") ?: 0
        )
    }
    
}