package xyz.xenondevs.nova.data.serialization.cbf.adapter

import io.netty.buffer.ByteBuf
import xyz.xenondevs.nova.data.serialization.cbf.BinaryAdapter
import xyz.xenondevs.nova.data.serialization.cbf.CBF
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

internal object MapBinaryAdapter : BinaryAdapter<Map<*, *>> {
    
    override fun write(obj: Map<*, *>, buf: ByteBuf) {
        buf.writeInt(obj.size)
        obj.forEach { (key, value) -> 
            CBF.write(key, buf)
            CBF.write(value, buf)
        }
    }
    
    override fun read(type: Type, buf: ByteBuf): Map<*, *> {
        val size = buf.readInt()
        val typeArguments = (type as ParameterizedType).actualTypeArguments
        val keyType = typeArguments[0]
        val valueType = typeArguments[1]
        
        val map = CBF.createInstance<MutableMap<Any?, Any?>>(type) ?: HashMap()
        repeat(size) { map[CBF.read(keyType, buf)] = CBF.read(valueType, buf) }
        
        return map
    }
    
}