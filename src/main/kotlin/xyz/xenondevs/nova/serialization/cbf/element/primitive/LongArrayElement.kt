package xyz.xenondevs.nova.serialization.cbf.element.primitive

import io.netty.buffer.ByteBuf
import xyz.xenondevs.nova.serialization.cbf.BackedElement
import xyz.xenondevs.nova.serialization.cbf.BinaryDeserializer

class LongArrayElement(override val value: LongArray) : BackedElement<LongArray> {
    override fun getTypeId() = 15.toByte()
    
    override fun write(buf: ByteBuf) {
        require(value.size <= 65535) { "Long array is too large!" }
        buf.writeShort(value.size)
        value.forEach(buf::writeLong)
    }
    
    override fun toString(): String {
        return value.contentToString()
    }
}

object LongArrayDeserializer : BinaryDeserializer<LongArrayElement> {
    override fun read(buf: ByteBuf): LongArrayElement {
        val array = LongArray(buf.readUnsignedShort())
        repeat(array.size) {
            array[it] = buf.readLong()
        }
        return LongArrayElement(array)
    }
}