package xyz.xenondevs.nova.data.serialization.cbf.element.primitive

import io.netty.buffer.ByteBuf
import xyz.xenondevs.nova.data.serialization.cbf.BackedElement
import xyz.xenondevs.nova.data.serialization.cbf.BinaryDeserializer

class CharArrayElement(override val value: CharArray) : BackedElement<CharArray> {
    override fun getTypeId() = 12
    
    override fun write(buf: ByteBuf) {
        require(value.size <= 65535) { "Char array is too large!" }
        buf.writeShort(value.size)
        value.map(Char::code).forEach(buf::writeChar)
    }
    
    override fun toString(): String {
        return value.contentToString()
    }
}

object CharArrayDeserializer : BinaryDeserializer<CharArrayElement> {
    override fun read(buf: ByteBuf): CharArrayElement {
        val array = CharArray(buf.readUnsignedShort())
        repeat(array.size) {
            array[it] = buf.readChar()
        }
        return CharArrayElement(array)
    }
}