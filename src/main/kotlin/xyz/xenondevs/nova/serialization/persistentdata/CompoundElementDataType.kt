package xyz.xenondevs.nova.serialization.persistentdata

import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataType
import xyz.xenondevs.nova.serialization.cbf.element.CompoundDeserializer
import xyz.xenondevs.nova.serialization.cbf.element.CompoundElement

object CompoundElementDataType: PersistentDataType<ByteArray, CompoundElement> {
    override fun getPrimitiveType() = ByteArray::class.java
    
    override fun getComplexType() = CompoundElement::class.java
    
    override fun toPrimitive(complex: CompoundElement, context: PersistentDataAdapterContext) = complex.toByteArray()
    
    override fun fromPrimitive(primitive: ByteArray, context: PersistentDataAdapterContext) = CompoundDeserializer.read(primitive)
    
}