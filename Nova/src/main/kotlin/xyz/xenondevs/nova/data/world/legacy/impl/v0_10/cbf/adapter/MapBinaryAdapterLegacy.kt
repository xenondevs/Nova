package xyz.xenondevs.nova.data.world.legacy.impl.v0_10.cbf.adapter

import io.netty.buffer.ByteBuf
import xyz.xenondevs.nova.data.world.legacy.impl.v0_10.cbf.BinaryAdapterLegacy
import xyz.xenondevs.nova.data.world.legacy.impl.v0_10.cbf.CBFLegacy
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

internal object MapBinaryAdapterLegacy : BinaryAdapterLegacy<Map<*, *>> {
    
    override fun write(obj: Map<*, *>, buf: ByteBuf) {
        buf.writeInt(obj.size)
        obj.forEach { (key, value) -> 
            CBFLegacy.write(key, buf)
            CBFLegacy.write(value, buf)
        }
    }
    
    override fun read(type: Type, buf: ByteBuf): Map<*, *> {
        val size = buf.readInt()
        val typeArguments = (type as ParameterizedType).actualTypeArguments
        val keyType = typeArguments[0]
        val valueType = typeArguments[1]
        
        val map = CBFLegacy.createInstance<MutableMap<Any?, Any?>>(type) ?: HashMap()
        repeat(size) { map[CBFLegacy.read(keyType, buf)] = CBFLegacy.read(valueType, buf) }
        
        return map
    }
    
}