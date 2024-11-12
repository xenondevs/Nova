package xyz.xenondevs.nova.serialization.json.serializer

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import xyz.xenondevs.commons.gson.getObject
import xyz.xenondevs.commons.gson.getString
import xyz.xenondevs.commons.gson.isString
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.util.getValue
import xyz.xenondevs.nova.world.block.state.NovaBlockState
import xyz.xenondevs.nova.world.block.state.property.ScopedBlockStateProperty
import java.lang.reflect.Type

internal object NovaBlockStateSerialization : JsonSerializer<NovaBlockState>, JsonDeserializer<NovaBlockState> {
    
    override fun serialize(src: NovaBlockState?, typeOfSrc: Type, context: JsonSerializationContext): JsonElement =
        if (src != null) serialize(src) else JsonNull.INSTANCE
    
    @Suppress("UNCHECKED_CAST")
    fun serialize(src: NovaBlockState): JsonElement {
        val block = src.block
        
        val obj = JsonObject()
        obj.addProperty("block", block.id.toString())
        
        val properties = JsonObject()
        obj.add("properties", properties)
        
        for (scopedProperty in block.stateProperties) {
            scopedProperty as ScopedBlockStateProperty<Any>
            
            val property = scopedProperty.property
            val value = src.values[property]!!
            
            properties.addProperty(property.id.toString(), scopedProperty.valueToString(value))
        }
        
        return obj
    }
    
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): NovaBlockState? =
        if (json is JsonObject) deserialize(json) else null
    
    fun deserialize(json: JsonObject): NovaBlockState? {
        val block = NovaRegistries.BLOCK.getValue(json.getString("block")) ?: return null
        val propertyMap = json.getObject("properties").entrySet().associate { (propertyName, value) ->
            require(value.isString()) { "Property value must be a string" }
            
            val scopedProperty = block.stateProperties.firstOrNull { it.property.id.toString() == propertyName }
                ?: return null
            
            if (!scopedProperty.isValidString(value.asString))
                return null
            
            scopedProperty.property to scopedProperty.stringToValue(value.asString)
        }
        return block.defaultBlockState.with(propertyMap)
    }
    
}