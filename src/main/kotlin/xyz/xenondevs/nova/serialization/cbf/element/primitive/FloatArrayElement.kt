package xyz.xenondevs.nova.serialization.cbf.element.primitive

import io.netty.buffer.ByteBuf
import xyz.xenondevs.nova.serialization.cbf.BackedElement
import xyz.xenondevs.nova.serialization.cbf.BinaryDeserializer

class FloatArrayElement(override val value: FloatArray) : BackedElement<FloatArray> {
    override fun getTypeId() = 14.toByte()
    
    override fun write(buf: ByteBuf) {
        require(value.size <= 65535) { "Float array is too large!" }
        buf.writeShort(value.size)
        value.forEach(buf::writeFloat)
    }
    
    override fun toString(): String {
        return value.contentToString()
    }
}

object FloatArrayDeserializer : BinaryDeserializer<FloatArrayElement> {
    override fun read(buf: ByteBuf): FloatArrayElement {
        val array = FloatArray(buf.readUnsignedShort())
        repeat(array.size) {
            array[it] = buf.readFloat()
        }
        return FloatArrayElement(array)
    }
}