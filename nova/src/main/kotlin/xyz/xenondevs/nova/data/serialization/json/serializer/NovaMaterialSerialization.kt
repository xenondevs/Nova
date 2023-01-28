package xyz.xenondevs.nova.data.serialization.json.serializer

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import xyz.xenondevs.nova.material.ItemNovaMaterial
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import java.lang.reflect.Type

internal object NovaMaterialSerialization : JsonSerializer<ItemNovaMaterial>, JsonDeserializer<ItemNovaMaterial> {
    
    override fun serialize(src: ItemNovaMaterial, typeOfSrc: Type, context: JsonSerializationContext) =
        JsonPrimitive(src.id.toString())
    
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext) =
        NovaMaterialRegistry.get(json.asString)
    
}