package xyz.xenondevs.nova.data.serialization.cbf.element.primitive

import io.netty.buffer.ByteBuf
import xyz.xenondevs.nova.data.serialization.cbf.BackedElement
import xyz.xenondevs.nova.data.serialization.cbf.BinaryDeserializer

class LongArrayElement(override val value: LongArray) : BackedElement<LongArray>() {
    
    override fun getTypeId() = 14
    
    override fun write(buf: ByteBuf) {
        require(value.size <= 65535) { "Long array is too large!" }
        buf.writeShort(value.size)
        value.forEach(buf::writeLong)
    }
    
    override fun toString(): String {
        return value.contentToString()
    }
    
    override fun equals(other: Any?): Boolean {
        return other is LongArrayElement && value.contentEquals(other.value)
    }
    
    override fun hashCode(): Int {
        return value.contentHashCode()
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