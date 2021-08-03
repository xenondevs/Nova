package xyz.xenondevs.nova.serialization.cbf.element.primitive

import io.netty.buffer.ByteBuf
import xyz.xenondevs.nova.serialization.cbf.BackedElement
import xyz.xenondevs.nova.serialization.cbf.BinaryDeserializer
import xyz.xenondevs.nova.serialization.cbf.Element
import xyz.xenondevs.nova.util.writeByte

class ByteElement(override val value: Byte) : BackedElement<Byte> {
    
    override fun getTypeId() = 2.toByte()
    
    override fun write(buf: ByteBuf) {
        buf.writeByte(value)
    }
    
    override fun toString() = value.toString()
    
}

object ByteDeserializer : BinaryDeserializer<ByteElement> {
    override fun read(buf: ByteBuf) = ByteElement(buf.readByte())
}