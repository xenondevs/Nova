package xyz.xenondevs.nova.data.serialization.cbf.adapter

import io.netty.buffer.ByteBuf
import xyz.xenondevs.nova.data.serialization.cbf.BinaryAdapter
import xyz.xenondevs.nova.data.serialization.cbf.CBF
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

internal object TripleBinaryAdapter : BinaryAdapter<Triple<*, *, *>> {
    
    override fun write(obj: Triple<*, *, *>, buf: ByteBuf) {
        CBF.write(obj.first, buf)
        CBF.write(obj.second, buf)
        CBF.write(obj.third, buf)
    }
    
    override fun read(type: Type, buf: ByteBuf): Triple<*, *, *> {
        val typeArguments = (type as ParameterizedType).actualTypeArguments
        
        return Triple<Any?, Any?, Any?>(
            CBF.read(typeArguments[0], buf),
            CBF.read(typeArguments[1], buf),
            CBF.read(typeArguments[2], buf)
        )
    }
    
}