package xyz.xenondevs.nova.data.serialization.cbf.element.primitive

import io.netty.buffer.ByteBuf
import xyz.xenondevs.nova.data.serialization.cbf.BackedElement
import xyz.xenondevs.nova.data.serialization.cbf.BinaryDeserializer

class DoubleElement(override val value: Double) : BackedElement<Double>() {
    
    override fun getTypeId() = 7
    
    override fun write(buf: ByteBuf) {
        buf.writeDouble(value)
    }
    
    override fun toString() = value.toString()
    
}

object DoubleDeserializer : BinaryDeserializer<DoubleElement> {
    override fun read(buf: ByteBuf) = DoubleElement(buf.readDouble())
}