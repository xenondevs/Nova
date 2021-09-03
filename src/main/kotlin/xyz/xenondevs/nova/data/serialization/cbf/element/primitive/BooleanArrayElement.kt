package xyz.xenondevs.nova.data.serialization.cbf.element.primitive

import io.netty.buffer.ByteBuf
import xyz.xenondevs.nova.data.serialization.cbf.BackedElement
import xyz.xenondevs.nova.data.serialization.cbf.BinaryDeserializer
import java.util.*

class BooleanArrayElement(override val value: BooleanArray) : BackedElement<BooleanArray>() {
    
    override fun getTypeId() = 9
    
    override fun write(buf: ByteBuf) {
        require(value.size <= 65535) { "Boolean array is too large!" }
        buf.writeShort(value.size)
        value.forEach(buf::writeBoolean)
    }
    
    override fun toString(): String {
        return value.contentToString()
    }
    
    override fun equals(other: Any?): Boolean {
        return other is BooleanArrayElement && value.contentEquals(other.value)
    }
    
    override fun hashCode(): Int {
        return value.contentHashCode()
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