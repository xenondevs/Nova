package xyz.xenondevs.nova.data.serialization.cbf.adapter

import io.netty.buffer.ByteBuf
import xyz.xenondevs.nova.data.serialization.cbf.BinaryAdapter
import xyz.xenondevs.nova.data.serialization.cbf.CBF
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

internal object PairBinaryAdapter : BinaryAdapter<Pair<*, *>> {
    
    override fun write(obj: Pair<*, *>, buf: ByteBuf) {
        CBF.write(obj.first, buf)
        CBF.write(obj.second, buf)
    }
    
    override fun read(type: Type, buf: ByteBuf): Pair<*, *> {
        val typeArguments = (type as ParameterizedType).actualTypeArguments
        
        return Pair<Any?, Any?>(
            CBF.read(typeArguments[0], buf),
            CBF.read(typeArguments[1], buf)
        )
    }
    
}