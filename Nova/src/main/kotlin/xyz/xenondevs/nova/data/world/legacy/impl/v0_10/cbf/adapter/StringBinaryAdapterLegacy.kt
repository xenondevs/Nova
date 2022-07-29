package xyz.xenondevs.nova.data.world.legacy.impl.v0_10.cbf.adapter

import io.netty.buffer.ByteBuf
import xyz.xenondevs.nova.data.world.legacy.impl.v0_10.cbf.BinaryAdapterLegacy
import xyz.xenondevs.nova.util.data.readStringLegacy
import xyz.xenondevs.nova.util.data.writeStringLegacy
import java.lang.reflect.Type

internal object StringBinaryAdapterLegacy : BinaryAdapterLegacy<String> {
    
    override fun write(obj: String, buf: ByteBuf) {
        buf.writeStringLegacy(obj)
    }
    
    override fun read(type: Type, buf: ByteBuf): String {
        return buf.readStringLegacy()
    }
    
}

internal object StringArrayBinaryAdapterLegacy : BinaryAdapterLegacy<Array<String>> {
    
    override fun write(obj: Array<String>, buf: ByteBuf) {
        buf.writeInt(obj.size)
        obj.forEach(buf::writeStringLegacy)
    }
    
    override fun read(type: Type, buf: ByteBuf): Array<String> {
        return Array(buf.readInt()) {buf.readStringLegacy()}
    }
    
}