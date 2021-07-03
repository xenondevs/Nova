package xyz.xenondevs.nova.serialization.persistentdata

import com.google.gson.JsonElement
import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataType
import xyz.xenondevs.nova.util.JSON_PARSER

object JsonElementDataType : PersistentDataType<String, JsonElement> {
    
    override fun getPrimitiveType() = String::class.java
    
    override fun getComplexType() = JsonElement::class.java
    
    override fun toPrimitive(complex: JsonElement, context: PersistentDataAdapterContext) = complex.toString()
    
    override fun fromPrimitive(primitive: String, context: PersistentDataAdapterContext): JsonElement = JSON_PARSER.parse(primitive)
    
}