package xyz.xenondevs.nova.data.world.legacy.impl.v0_10.cbf.adapter

import io.netty.buffer.ByteBuf
import xyz.xenondevs.nova.data.world.legacy.impl.v0_10.cbf.BinaryAdapterLegacy
import xyz.xenondevs.nova.data.world.legacy.impl.v0_10.cbf.CBFLegacy
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

internal object TripleBinaryAdapterLegacy : BinaryAdapterLegacy<Triple<*, *, *>> {
    
    override fun write(obj: Triple<*, *, *>, buf: ByteBuf) {
        CBFLegacy.write(obj.first, buf)
        CBFLegacy.write(obj.second, buf)
        CBFLegacy.write(obj.third, buf)
    }
    
    override fun read(type: Type, buf: ByteBuf): Triple<*, *, *> {
        val typeArguments = (type as ParameterizedType).actualTypeArguments
        
        return Triple<Any?, Any?, Any?>(
            CBFLegacy.read(typeArguments[0], buf),
            CBFLegacy.read(typeArguments[1], buf),
            CBFLegacy.read(typeArguments[2], buf)
        )
    }
    
}