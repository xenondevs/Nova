package xyz.xenondevs.nova.data.serialization.persistentdata

import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataType
import xyz.xenondevs.nova.data.serialization.cbf.CBF
import xyz.xenondevs.nova.data.serialization.cbf.Compound

object CompoundDataType : PersistentDataType<ByteArray, Compound> {
    
    override fun getPrimitiveType() = ByteArray::class.java
    
    override fun getComplexType() = Compound::class.java
    
    override fun toPrimitive(complex: Compound, context: PersistentDataAdapterContext) = CBF.write(complex)
    
    override fun fromPrimitive(primitive: ByteArray, context: PersistentDataAdapterContext) = CBF.read<Compound>(primitive)!!
    
}