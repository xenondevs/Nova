package xyz.xenondevs.nova.serialization.cbf.element.primitive

import io.netty.buffer.ByteBuf
import xyz.xenondevs.nova.serialization.cbf.BackedElement
import xyz.xenondevs.nova.serialization.cbf.BinaryDeserializer
import xyz.xenondevs.nova.serialization.cbf.Element

class LongElement(override val value: Long): BackedElement<Long> {
    
    override fun getTypeId() = 6
    
    override fun write(buf: ByteBuf) {
        buf.writeLong(value)
    }
    
    override fun toString() = value.toString()
    
}

object LongDeserializer : BinaryDeserializer<LongElement> {
    override fun read(buf: ByteBuf) = LongElement(buf.readLong())
}