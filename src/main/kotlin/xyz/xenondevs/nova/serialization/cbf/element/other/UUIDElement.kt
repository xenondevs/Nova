package xyz.xenondevs.nova.serialization.cbf.element.other

import io.netty.buffer.ByteBuf
import xyz.xenondevs.nova.serialization.cbf.BackedElement
import xyz.xenondevs.nova.serialization.cbf.BinaryDeserializer
import java.util.*

class UUIDElement(override val value: UUID) : BackedElement<UUID> {
    override fun getTypeId() = 18.toByte()
    
    override fun write(buf: ByteBuf) {
        buf.writeLong(value.mostSignificantBits)
        buf.writeLong(value.leastSignificantBits)
    }
    
    override fun toString() = value.toString()
    
}

object UUIDDeserializer : BinaryDeserializer<UUIDElement> {
    override fun read(buf: ByteBuf) = UUIDElement(UUID(buf.readLong(), buf.readLong()))
}