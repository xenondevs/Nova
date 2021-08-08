package xyz.xenondevs.nova.serialization.cbf.element.primitive

import io.netty.buffer.ByteBuf
import xyz.xenondevs.nova.serialization.cbf.BackedElement
import xyz.xenondevs.nova.serialization.cbf.BinaryDeserializer

class FloatElement(override val value: Float) : BackedElement<Float> {
    
    override fun getTypeId() = 5
    
    override fun write(buf: ByteBuf) {
        buf.writeFloat(value)
    }
    
    override fun toString() = value.toString()
    
}

object FloatDeserializer : BinaryDeserializer<FloatElement> {
    override fun read(buf: ByteBuf) = FloatElement(buf.readFloat())
}