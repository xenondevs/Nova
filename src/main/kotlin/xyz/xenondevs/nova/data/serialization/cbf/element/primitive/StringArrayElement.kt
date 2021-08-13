package xyz.xenondevs.nova.data.serialization.cbf.element.primitive

import io.netty.buffer.ByteBuf
import xyz.xenondevs.nova.data.serialization.cbf.BackedElement
import xyz.xenondevs.nova.data.serialization.cbf.BinaryDeserializer
import xyz.xenondevs.nova.util.data.readString
import xyz.xenondevs.nova.util.data.writeString

class StringArrayElement(override val value: Array<String>) : BackedElement<Array<String>> {
    override fun getTypeId() = 16
    
    override fun write(buf: ByteBuf) {
        require(value.size <= 65535) { "String array is too large!" }
        buf.writeShort(value.size)
        value.forEach(buf::writeString)
    }
    
    override fun toString(): String {
        return value.contentToString()
    }
}

object StringArrayDeserializer : BinaryDeserializer<StringArrayElement> {
    override fun read(buf: ByteBuf): StringArrayElement {
        val array = Array(buf.readUnsignedShort()) { "" }
        repeat(array.size) {
            array[it] = buf.readString()
        }
        return StringArrayElement(array)
    }
}