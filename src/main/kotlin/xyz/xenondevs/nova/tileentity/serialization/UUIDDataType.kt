package xyz.xenondevs.nova.tileentity.serialization

import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataType
import java.util.*

object UUIDDataType : PersistentDataType<String, UUID> {
    
    override fun getPrimitiveType() = String::class.java
    
    override fun getComplexType() = UUID::class.javaObjectType
    
    override fun toPrimitive(complex: UUID, context: PersistentDataAdapterContext) = complex.toString()
    
    override fun fromPrimitive(primitive: String, context: PersistentDataAdapterContext) = UUID.fromString(primitive)!!
    
}