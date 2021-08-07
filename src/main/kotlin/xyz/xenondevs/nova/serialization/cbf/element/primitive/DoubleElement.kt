package xyz.xenondevs.nova.serialization.cbf.element.primitive

import io.netty.buffer.ByteBuf
import xyz.xenondevs.nova.serialization.cbf.BackedElement
import xyz.xenondevs.nova.serialization.cbf.BinaryDeserializer
import xyz.xenondevs.nova.serialization.cbf.Element

class DoubleElement(override val value: Double) : BackedElement<Double> {
    
    override fun getTypeId() = 7
    
    override fun write(buf: ByteBuf) {
        buf.writeDouble(value)
    }
    
    override fun toString() = value.toString()
    
}

object DoubleDeserializer : BinaryDeserializer<DoubleElement> {
    override fun read(buf: ByteBuf) = DoubleElement(buf.readDouble())
}