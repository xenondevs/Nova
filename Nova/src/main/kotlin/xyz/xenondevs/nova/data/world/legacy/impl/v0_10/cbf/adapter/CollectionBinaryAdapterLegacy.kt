package xyz.xenondevs.nova.data.world.legacy.impl.v0_10.cbf.adapter

import io.netty.buffer.ByteBuf
import xyz.xenondevs.nova.data.world.legacy.impl.v0_10.cbf.BinaryAdapterLegacy
import xyz.xenondevs.nova.data.world.legacy.impl.v0_10.cbf.CBFLegacy
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

internal object CollectionBinaryAdapterLegacy : BinaryAdapterLegacy<Collection<*>> {
    
    override fun write(obj: Collection<*>, buf: ByteBuf) {
        buf.writeInt(obj.size)
        obj.forEach { CBFLegacy.write(it, buf) }
    }
    
    override fun read(type: Type, buf: ByteBuf): Collection<*> {
        val size = buf.readInt()
        val valueType = (type as ParameterizedType).actualTypeArguments[0]
        val collection = CBFLegacy.createInstance<MutableCollection<Any?>>(type) ?: ArrayList()
        repeat(size) { collection.add(CBFLegacy.read(valueType, buf)) }
        return collection
    }
    
}