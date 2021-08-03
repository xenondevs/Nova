package xyz.xenondevs.nova.serialization.cbf.element.primitive

import io.netty.buffer.ByteBuf
import xyz.xenondevs.nova.serialization.cbf.BackedElement
import xyz.xenondevs.nova.serialization.cbf.BinaryDeserializer
import xyz.xenondevs.nova.serialization.cbf.Element

class CharElement(override val value: Char) : BackedElement<Char> {
    
    override fun getTypeId() = 4.toByte()
    
    override fun write(buf: ByteBuf) {
        buf.writeChar(value.code)
    }
    
    override fun toString() = value.toString()
    
}

object CharDeserializer : BinaryDeserializer<CharElement> {
    override fun read(buf: ByteBuf) = CharElement(buf.readChar())
}