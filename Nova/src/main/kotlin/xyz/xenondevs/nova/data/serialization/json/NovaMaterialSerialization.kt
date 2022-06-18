package xyz.xenondevs.nova.data.serialization.json

import com.google.gson.*
import xyz.xenondevs.nova.material.ItemNovaMaterial
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import java.lang.reflect.Type

internal object NovaMaterialSerialization : JsonSerializer<ItemNovaMaterial>, JsonDeserializer<ItemNovaMaterial> {
    
    override fun serialize(src: ItemNovaMaterial, typeOfSrc: Type, context: JsonSerializationContext) =
        JsonPrimitive(src.id.toString())
    
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext) =
        NovaMaterialRegistry.get(json.asString)
    
}