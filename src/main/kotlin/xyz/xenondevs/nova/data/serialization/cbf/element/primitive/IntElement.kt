package xyz.xenondevs.nova.data.serialization.cbf.element.primitive

import io.netty.buffer.ByteBuf
import xyz.xenondevs.nova.data.serialization.cbf.BackedElement
import xyz.xenondevs.nova.data.serialization.cbf.BinaryDeserializer

class IntElement(override val value: Int) : BackedElement<Int>() {
    
    override fun getTypeId() = 3
    
    override fun write(buf: ByteBuf) {
        buf.writeInt(value)
    }
    
    override fun toString() = value.toString()
    
}

object IntDeserializer : BinaryDeserializer<IntElement> {
    override fun read(buf: ByteBuf) = IntElement(buf.readInt())
}