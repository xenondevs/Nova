package xyz.xenondevs.nova.data.world.legacy.impl.v0_10.cbf.adapter

import io.netty.buffer.ByteBuf
import xyz.xenondevs.nova.data.world.legacy.impl.v0_10.cbf.BinaryAdapterLegacy
import java.awt.Color
import java.lang.reflect.Type

internal object ColorBinaryAdapterLegacy : BinaryAdapterLegacy<Color> {
    
    override fun write(obj: Color, buf: ByteBuf) {
        buf.writeInt(obj.rgb)
    }
    
    override fun read(type: Type, buf: ByteBuf): Color {
        return Color(buf.readInt(), true)
    }
    
}