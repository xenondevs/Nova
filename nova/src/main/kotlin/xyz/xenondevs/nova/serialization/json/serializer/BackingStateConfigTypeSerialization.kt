package xyz.xenondevs.nova.serialization.json.serializer

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import xyz.xenondevs.nova.world.block.state.model.BackingStateConfigType
import java.lang.reflect.Type
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.jvm.jvmName

internal object BackingStateConfigTypeSerialization : JsonSerializer<BackingStateConfigType<*>>, JsonDeserializer<BackingStateConfigType<*>> {
    
    override fun serialize(src: BackingStateConfigType<*>, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return JsonPrimitive(src::class.jvmName)
    }
    
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): BackingStateConfigType<*> {
        val kclass = Class.forName(json.asString).kotlin
        return (kclass.objectInstance ?: kclass.companionObjectInstance) as BackingStateConfigType<*>
    }
    
}