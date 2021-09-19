package xyz.xenondevs.nova.data.serialization.cbf.element.primitive

import io.netty.buffer.ByteBuf
import xyz.xenondevs.nova.data.serialization.cbf.BackedElement
import xyz.xenondevs.nova.data.serialization.cbf.BinaryDeserializer

class ByteArrayElement(override val value: ByteArray) : BackedElement<ByteArray>() {
    
    override fun getTypeId() = 10
    
    override fun write(buf: ByteBuf) {
        require(value.size <= 65535) { "Byte array is too large!" }
        buf.writeShort(value.size)
        buf.writeBytes(value)
    }
    
    override fun toString(): String {
        return value.contentToString()
    }
    
    override fun equals(other: Any?): Boolean {
        return other is ByteArrayElement && value.contentEquals(other.value)
    }
    
    override fun hashCode(): Int {
        return value.contentHashCode()
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