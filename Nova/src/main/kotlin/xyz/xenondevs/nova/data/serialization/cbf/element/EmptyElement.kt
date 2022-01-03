package xyz.xenondevs.nova.data.serialization.cbf.element

import io.netty.buffer.ByteBuf
import xyz.xenondevs.nova.data.serialization.cbf.BinaryDeserializer
import xyz.xenondevs.nova.data.serialization.cbf.Element

object EmptyElement : Element {
    override fun getTypeId() = 0
    
    override fun write(buf: ByteBuf) = Unit
}

object EmptyDeserializer : BinaryDeserializer<EmptyElement> {
    override fun read(buf: ByteBuf) = EmptyElement
}