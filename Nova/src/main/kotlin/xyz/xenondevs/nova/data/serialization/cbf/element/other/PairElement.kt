package xyz.xenondevs.nova.data.serialization.cbf.element.other

import io.netty.buffer.ByteBuf
import xyz.xenondevs.nova.data.serialization.cbf.BackedElement
import xyz.xenondevs.nova.data.serialization.cbf.BinaryDeserializer
import xyz.xenondevs.nova.data.serialization.cbf.DeserializerRegistry

class PairElement(override val value: Pair<Any, Any>) : BackedElement<Pair<Any, Any>>() {
    
    override fun getTypeId() = 28
    
    override fun write(buf: ByteBuf) {
        val firstElement = createElement(value.first)
        val secondElement = createElement(value.second)
        
        buf.writeByte(firstElement.getTypeId())
        firstElement.write(buf)
        
        buf.writeByte(secondElement.getTypeId())
        secondElement.write(buf)
    }
    
}

object PairDeserializer : BinaryDeserializer<PairElement> {
    
    @Suppress("UNCHECKED_CAST")
    override fun read(buf: ByteBuf): PairElement {
        val first = DeserializerRegistry.getForType(buf.readByte())!!.read(buf) as BackedElement<Any>
        val second = DeserializerRegistry.getForType(buf.readByte())!!.read(buf) as BackedElement<Any>
        return PairElement(first.value to second.value)
    }
    
}