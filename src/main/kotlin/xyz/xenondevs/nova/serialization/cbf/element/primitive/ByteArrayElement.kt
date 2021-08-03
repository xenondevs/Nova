package xyz.xenondevs.nova.serialization.cbf.element.primitive

import io.netty.buffer.ByteBuf
import xyz.xenondevs.nova.serialization.cbf.BackedElement
import xyz.xenondevs.nova.serialization.cbf.BinaryDeserializer

class ByteArrayElement(override val value: ByteArray) : BackedElement<ByteArray> {
    override fun getTypeId() = 11.toByte()
    
    override fun write(buf: ByteBuf) {
        require(value.size <= 65535) { "Byte array is too large!" }
        buf.writeShort(value.size)
        buf.writeBytes(value)
    }
    
    override fun toString(): String {
        return value.contentToString()
    }
}

object ByteArrayDeserializer : BinaryDeserializer<ByteArrayElement> {
    override fun read(buf: ByteBuf): ByteArrayElement {
        val array = ByteArray(buf.readUnsignedShort())
        repeat(array.size) {
            array[it] = buf.readByte()
        }
        return ByteArrayElement(array)
    }
}