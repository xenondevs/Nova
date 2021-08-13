package xyz.xenondevs.nova.data.serialization.cbf.element.primitive

import io.netty.buffer.ByteBuf
import xyz.xenondevs.nova.data.serialization.cbf.BackedElement
import xyz.xenondevs.nova.data.serialization.cbf.BinaryDeserializer
import xyz.xenondevs.nova.util.data.writeByte

class ByteElement(override val value: Byte) : BackedElement<Byte> {
    
    override fun getTypeId() = 2
    
    override fun write(buf: ByteBuf) {
        buf.writeByte(value)
    }
    
    override fun toString() = value.toString()
    
}

object ByteDeserializer : BinaryDeserializer<ByteElement> {
    override fun read(buf: ByteBuf) = ByteElement(buf.readByte())
}