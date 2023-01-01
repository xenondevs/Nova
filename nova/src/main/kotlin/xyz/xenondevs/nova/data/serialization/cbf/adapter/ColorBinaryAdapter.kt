package xyz.xenondevs.nova.data.serialization.cbf.adapter

import xyz.xenondevs.cbf.adapter.BinaryAdapter
import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.cbf.io.ByteWriter
import java.awt.Color
import java.lang.reflect.Type

internal object ColorBinaryAdapter : BinaryAdapter<Color> {
    
    override fun read(type: Type, reader: ByteReader): Color {
        return Color(reader.readInt(), true)
    }
    
    override fun write(obj: Color, writer: ByteWriter) {
        writer.writeInt(obj.rgb)
    }
    
}