package xyz.xenondevs.nova.serialization.cbf.element.primitive

import io.netty.buffer.ByteBuf
import xyz.xenondevs.nova.serialization.cbf.BackedElement
import xyz.xenondevs.nova.serialization.cbf.BinaryDeserializer
import xyz.xenondevs.nova.serialization.cbf.Element

class FloatElement(override val value: Float) : BackedElement<Float> {
    
    override fun getTypeId() = 5.toByte()
    
    override fun write(buf: ByteBuf) {
        buf.writeFloat(value)
    }
    
    override fun toString() = value.toString()
    
}

object FloatDeserializer : BinaryDeserializer<FloatElement> {
    override fun read(buf: ByteBuf) = FloatElement(buf.readFloat())
}