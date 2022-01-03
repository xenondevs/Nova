package xyz.xenondevs.nova.data.serialization.json

import com.google.gson.*
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import java.lang.reflect.Type

object NovaMaterialSerialization : JsonSerializer<NovaMaterial>, JsonDeserializer<NovaMaterial> {
    
    override fun serialize(src: NovaMaterial, typeOfSrc: Type, context: JsonSerializationContext) =
        JsonPrimitive(src.typeName)
    
    
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext) =
        NovaMaterialRegistry.get(json.asString)
    
}