package xyz.xenondevs.nova.data.serialization.cbf.element.primitive

import io.netty.buffer.ByteBuf
import xyz.xenondevs.nova.data.serialization.cbf.BackedElement
import xyz.xenondevs.nova.data.serialization.cbf.BinaryDeserializer

class BooleanElement(override val value: Boolean) : BackedElement<Boolean> {
    
    override fun getTypeId() = 1
    
    override fun write(buf: ByteBuf) {
        buf.writeBoolean(value)
    }
    
    override fun toString() = value.toString()
    
}

object BooleanDeserializer : BinaryDeserializer<BooleanElement> {
    override fun read(buf: ByteBuf) = BooleanElement(buf.readBoolean())
}