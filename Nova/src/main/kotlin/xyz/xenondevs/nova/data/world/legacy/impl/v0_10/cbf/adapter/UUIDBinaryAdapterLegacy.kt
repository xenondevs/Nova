package xyz.xenondevs.nova.data.world.legacy.impl.v0_10.cbf.adapter

import io.netty.buffer.ByteBuf
import xyz.xenondevs.nova.data.world.legacy.impl.v0_10.cbf.BinaryAdapterLegacy
import xyz.xenondevs.nova.util.data.readUUID
import xyz.xenondevs.nova.util.data.writeUUID
import java.lang.reflect.Type
import java.util.*

internal object UUIDBinaryAdapterLegacy : BinaryAdapterLegacy<UUID> {
    
    override fun write(obj: UUID, buf: ByteBuf) {
        buf.writeUUID(obj)
    }
    
    override fun read(type: Type, buf: ByteBuf): UUID {
        return buf.readUUID()
    }
    
}