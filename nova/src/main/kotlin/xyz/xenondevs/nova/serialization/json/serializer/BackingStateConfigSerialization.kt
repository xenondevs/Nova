package xyz.xenondevs.nova.serialization.json.serializer

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import xyz.xenondevs.commons.gson.getBoolean
import xyz.xenondevs.commons.gson.getInt
import xyz.xenondevs.commons.gson.getString
import xyz.xenondevs.nova.world.block.state.model.BackingStateConfig
import xyz.xenondevs.nova.world.block.state.model.BackingStateConfigType
import java.lang.reflect.Type
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.jvm.jvmName

internal object BackingStateConfigSerialization : JsonSerializer<BackingStateConfig>, JsonDeserializer<BackingStateConfig> {
    
    override fun serialize(src: BackingStateConfig, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val obj = JsonObject()
        obj.addProperty("type", src::class.jvmName)
        obj.addProperty("id", src.id)
        obj.addProperty("waterlogged", src.waterlogged)
        return obj
    }
    
    @Suppress("UNCHECKED_CAST")
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): BackingStateConfig {
        json as JsonObject
        val kclass = Class.forName(json.getString("type")).kotlin
        val type = (kclass.objectInstance ?: kclass.companionObjectInstance) as BackingStateConfigType<BackingStateConfig>
        return type.of(json.getInt("id"), json.getBoolean("waterlogged"))
    }
    
}