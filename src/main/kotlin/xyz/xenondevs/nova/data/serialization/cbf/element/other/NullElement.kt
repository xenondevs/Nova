package xyz.xenondevs.nova.data.serialization.cbf.element.other

import io.netty.buffer.ByteBuf
import xyz.xenondevs.nova.data.serialization.cbf.BackedElement
import xyz.xenondevs.nova.data.serialization.cbf.BinaryDeserializer

object NullElement : BackedElement<Any?>() {
    override val value: Any? = null
    
    override fun getTypeId() = 17
    
    override fun write(buf: ByteBuf) = Unit
    
    override fun toString() = "null"
}

object NullDeserializer : BinaryDeserializer<NullElement> {
    override fun read(buf: ByteBuf) = NullElement
}