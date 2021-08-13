package xyz.xenondevs.nova.data.serialization.cbf.element.primitive

import io.netty.buffer.ByteBuf
import xyz.xenondevs.nova.data.serialization.cbf.BackedElement
import xyz.xenondevs.nova.data.serialization.cbf.BinaryDeserializer
import xyz.xenondevs.nova.util.data.readString
import xyz.xenondevs.nova.util.data.writeString

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