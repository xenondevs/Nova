package xyz.xenondevs.nova.serialization.cbf.element.primitive

import io.netty.buffer.ByteBuf
import xyz.xenondevs.nova.serialization.cbf.BackedElement
import xyz.xenondevs.nova.serialization.cbf.BinaryDeserializer
import xyz.xenondevs.nova.util.readString
import xyz.xenondevs.nova.util.writeString

class StringElement(override val value: String) : BackedElement<String> {
    override fun getTypeId() = 8
    
    override fun write(buf: ByteBuf) {
        buf.writeString(value)
    }
    
    override fun toString(): String {
        return value
    }
    
}

object StringDeserializer : BinaryDeserializer<StringElement> {
    override fun read(buf: ByteBuf) = StringElement(buf.readString())
}