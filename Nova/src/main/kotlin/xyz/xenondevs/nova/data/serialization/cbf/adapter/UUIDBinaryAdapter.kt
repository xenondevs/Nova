package xyz.xenondevs.nova.data.serialization.cbf.adapter

import io.netty.buffer.ByteBuf
import xyz.xenondevs.nova.data.serialization.cbf.BinaryAdapter
import xyz.xenondevs.nova.util.data.readUUID
import xyz.xenondevs.nova.util.data.writeUUID
import java.lang.reflect.Type
import java.util.*

internal object UUIDBinaryAdapter : BinaryAdapter<UUID> {
    
    override fun write(obj: UUID, buf: ByteBuf) {
        buf.writeUUID(obj)
    }
    
    override fun read(type: Type, buf: ByteBuf): UUID {
        return buf.readUUID()
    }
    
}