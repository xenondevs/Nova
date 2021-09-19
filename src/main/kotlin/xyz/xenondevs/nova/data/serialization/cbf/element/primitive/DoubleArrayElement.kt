package xyz.xenondevs.nova.data.serialization.cbf.element.primitive

import io.netty.buffer.ByteBuf
import xyz.xenondevs.nova.data.serialization.cbf.BackedElement
import xyz.xenondevs.nova.data.serialization.cbf.BinaryDeserializer

class DoubleArrayElement(override val value: DoubleArray) : BackedElement<DoubleArray>() {
    
    override fun getTypeId() = 15
    
    override fun write(buf: ByteBuf) {
        require(value.size <= 65535) { "Double array is too large!" }
        buf.writeShort(value.size)
        value.forEach(buf::writeDouble)
    }
    
    override fun toString(): String {
        return value.contentToString()
    }
    
    override fun equals(other: Any?): Boolean {
        return other is DoubleArrayElement && value.contentEquals(other.value)
    }
    
    override fun hashCode(): Int {
        return value.contentHashCode()
    }
    
}

object DoubleArrayDeserializer : BinaryDeserializer<DoubleArrayElement> {
    override fun read(buf: ByteBuf): DoubleArrayElement {
        val array = DoubleArray(buf.readUnsignedShort())
        repeat(array.size) {
            array[it] = buf.readDouble()
        }
        return DoubleArrayElement(array)
    }
}