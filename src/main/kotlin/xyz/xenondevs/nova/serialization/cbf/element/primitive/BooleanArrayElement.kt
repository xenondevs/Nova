package xyz.xenondevs.nova.serialization.cbf.element.primitive

import io.netty.buffer.ByteBuf
import xyz.xenondevs.nova.serialization.cbf.BackedElement
import xyz.xenondevs.nova.serialization.cbf.BinaryDeserializer

class BooleanArrayElement(override val value: BooleanArray) : BackedElement<BooleanArray> {
    override fun getTypeId() = 9
    
    override fun write(buf: ByteBuf) {
        require(value.size <= 65535) { "Boolean array is too large!" }
        buf.writeShort(value.size)
        value.forEach(buf::writeBoolean)
    }
    
    override fun toString(): String {
        return value.contentToString()
    }
    
}

object BooleanArrayDeserializer : BinaryDeserializer<BooleanArrayElement> {
    override fun read(buf: ByteBuf): BooleanArrayElement {
        val array = BooleanArray(buf.readUnsignedShort())
        repeat(array.size) {
            array[it] = buf.readBoolean()
        }
        return BooleanArrayElement(array)
    }
}