package xyz.xenondevs.nova.serialization.json.serializer

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import xyz.xenondevs.nova.serialization.json.getDeserialized
import xyz.xenondevs.nova.world.block.state.model.BackingStateBlockModelProvider
import xyz.xenondevs.nova.world.block.state.model.DisplayEntityBlockModelProvider
import xyz.xenondevs.nova.world.block.state.model.LinkedBlockModelProvider
import xyz.xenondevs.nova.world.block.state.model.ModelLessBlockModelProvider
import java.lang.reflect.Type

internal object LinkedBlockModelProviderSerialization : JsonSerializer<LinkedBlockModelProvider<*>>, JsonDeserializer<LinkedBlockModelProvider<*>> {
    
    override fun serialize(src: LinkedBlockModelProvider<*>, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val obj = JsonObject()
        
        obj.addProperty("provider_type", when (src.provider) {
            BackingStateBlockModelProvider -> "backing_state"
            DisplayEntityBlockModelProvider -> "display_entity"
            ModelLessBlockModelProvider -> "model_less"
        })
        
        obj.add("info", context.serialize(src.info))
        
        return obj
    }
    
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): LinkedBlockModelProvider<*> {
        json as JsonObject
        
        return when (val providerName = json["provider_type"].asString) {
            "backing_state" -> LinkedBlockModelProvider(BackingStateBlockModelProvider, json.getDeserialized("info"))
            "display_entity" -> LinkedBlockModelProvider(DisplayEntityBlockModelProvider, json.getDeserialized("info"))
            "model_less" -> LinkedBlockModelProvider(ModelLessBlockModelProvider, json.getDeserialized("info"))
            else -> throw IllegalArgumentException("Unknown provider type $providerName}")
        }
    }
    
}