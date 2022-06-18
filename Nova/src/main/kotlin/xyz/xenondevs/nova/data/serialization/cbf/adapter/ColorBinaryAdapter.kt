package xyz.xenondevs.nova.data.serialization.cbf.adapter

import io.netty.buffer.ByteBuf
import xyz.xenondevs.nova.data.serialization.cbf.BinaryAdapter
import java.awt.Color
import java.lang.reflect.Type

internal object ColorBinaryAdapter : BinaryAdapter<Color> {
    
    override fun write(obj: Color, buf: ByteBuf) {
        buf.writeInt(obj.rgb)
    }
    
    override fun read(type: Type, buf: ByteBuf): Color {
        return Color(buf.readInt(), true)
    }
    
}