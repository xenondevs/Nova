package xyz.xenondevs.nova.serialization.cbf.adapter

import xyz.xenondevs.cbf.adapter.BinaryAdapter
import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.cbf.io.ByteWriter
import java.awt.Color
import kotlin.reflect.KType

internal object ColorBinaryAdapter : BinaryAdapter<Color> {
    
    override fun read(type: KType, reader: ByteReader): Color {
        return Color(reader.readInt(), true)
    }
    
    override fun write(obj: Color, type: KType, writer: ByteWriter) {
        writer.writeInt(obj.rgb)
    }
    
    override fun copy(obj: Color, type: KType): Color {
        return Color(obj.rgb, true)
    }
    
}