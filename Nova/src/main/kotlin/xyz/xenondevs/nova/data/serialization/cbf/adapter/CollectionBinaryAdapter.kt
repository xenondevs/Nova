package xyz.xenondevs.nova.data.serialization.cbf.adapter

import io.netty.buffer.ByteBuf
import xyz.xenondevs.nova.data.serialization.cbf.BinaryAdapter
import xyz.xenondevs.nova.data.serialization.cbf.CBF
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

internal object CollectionBinaryAdapter : BinaryAdapter<Collection<*>> {
    
    override fun write(obj: Collection<*>, buf: ByteBuf) {
        buf.writeInt(obj.size)
        obj.forEach { CBF.write(it, buf) }
    }
    
    override fun read(type: Type, buf: ByteBuf): Collection<*> {
        val size = buf.readInt()
        val valueType = (type as ParameterizedType).actualTypeArguments[0]
        val collection = CBF.createInstance<MutableCollection<Any?>>(type) ?: ArrayList()
        repeat(size) { collection.add(CBF.read(valueType, buf)) }
        return collection
    }
    
}