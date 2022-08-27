package xyz.xenondevs.nova.data.serialization.cbf.adapter

import xyz.xenondevs.cbf.adapter.BinaryAdapter
import xyz.xenondevs.cbf.buffer.ByteBuffer
import java.awt.Color
import java.lang.reflect.Type

internal object ColorBinaryAdapter : BinaryAdapter<Color> {
    
    override fun read(type: Type, buf: ByteBuffer): Color {
        return Color(buf.readInt(), true)
    }
    
    override fun write(obj: Color, buf: ByteBuffer) {
        buf.writeInt(obj.rgb)
    }
    
}