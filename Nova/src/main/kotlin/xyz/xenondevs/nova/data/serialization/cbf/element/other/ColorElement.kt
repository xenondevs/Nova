package xyz.xenondevs.nova.data.serialization.cbf.element.other

import io.netty.buffer.ByteBuf
import xyz.xenondevs.nova.data.serialization.cbf.BackedElement
import xyz.xenondevs.nova.data.serialization.cbf.BinaryDeserializer
import java.awt.Color

class ColorElement(override val value: Color) : BackedElement<Color>() {
    
    override fun getTypeId() = 27
    
    override fun write(buf: ByteBuf) {
        buf.writeInt(value.rgb)
    }
    
    override fun toString(): String {
        return "r=${value.red}, g=${value.green}, b=${value.blue}, a=${value.alpha}"
    }
    
}

object ColorDeserializer : BinaryDeserializer<ColorElement> {
    
    override fun read(buf: ByteBuf): ColorElement {
        return ColorElement(Color(buf.readInt()))
    }
    
}
