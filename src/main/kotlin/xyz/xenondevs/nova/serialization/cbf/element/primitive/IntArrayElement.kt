package xyz.xenondevs.nova.serialization.cbf.element.primitive

import io.netty.buffer.ByteBuf
import xyz.xenondevs.nova.serialization.cbf.BackedElement
import xyz.xenondevs.nova.serialization.cbf.BinaryDeserializer

class IntArrayElement(override val value: IntArray) : BackedElement<IntArray> {
    override fun getTypeId() = 11
    
    override fun write(buf: ByteBuf) {
        require(value.size <= 65535) { "Int array is too large!" }
        buf.writeShort(value.size)
        value.forEach(buf::writeInt)
    }
    
    override fun toString(): String {
        return value.contentToString()
    }
}

object IntArrayDeserializer : BinaryDeserializer<IntArrayElement> {
    override fun read(buf: ByteBuf): IntArrayElement {
        val array = IntArray(buf.readUnsignedShort())
        repeat(array.size) {
            array[it] = buf.readInt()
        }
        return IntArrayElement(array)
    }
}