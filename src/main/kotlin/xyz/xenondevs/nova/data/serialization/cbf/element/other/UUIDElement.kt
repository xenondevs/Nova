package xyz.xenondevs.nova.data.serialization.cbf.element.other

import io.netty.buffer.ByteBuf
import xyz.xenondevs.nova.data.serialization.cbf.BackedElement
import xyz.xenondevs.nova.data.serialization.cbf.BinaryDeserializer
import java.util.*

class UUIDElement(override val value: UUID) : BackedElement<UUID> {
    override fun getTypeId() = 21
    
    override fun write(buf: ByteBuf) {
        buf.writeLong(value.mostSignificantBits)
        buf.writeLong(value.leastSignificantBits)
    }
    
    override fun toString() = value.toString()
    
}

object UUIDDeserializer : BinaryDeserializer<UUIDElement> {
    override fun read(buf: ByteBuf) = UUIDElement(UUID(buf.readLong(), buf.readLong()))
}