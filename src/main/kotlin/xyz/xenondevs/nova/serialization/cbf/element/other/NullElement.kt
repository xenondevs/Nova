package xyz.xenondevs.nova.serialization.cbf.element.other

import io.netty.buffer.ByteBuf
import xyz.xenondevs.nova.serialization.cbf.BackedElement
import xyz.xenondevs.nova.serialization.cbf.BinaryDeserializer

object NullElement : BackedElement<Any?> {
    override val value: Any? = null
    
    override fun getTypeId() = 21.toByte()
    
    override fun write(buf: ByteBuf) = Unit
    
    override fun toString() = "null"
}

object NullDeserializer : BinaryDeserializer<NullElement> {
    override fun read(buf: ByteBuf) = NullElement
}