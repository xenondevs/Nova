package xyz.xenondevs.nova.serialization

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataType

object JsonElementDataType : PersistentDataType<String, JsonElement> {
    
    override fun getPrimitiveType() = String::class.java
    
    override fun getComplexType() = JsonElement::class.java
    
    override fun toPrimitive(complex: JsonElement, context: PersistentDataAdapterContext) = complex.toString()
    
    override fun fromPrimitive(primitive: String, context: PersistentDataAdapterContext): JsonElement = JsonParser().parse(primitive)
    
}