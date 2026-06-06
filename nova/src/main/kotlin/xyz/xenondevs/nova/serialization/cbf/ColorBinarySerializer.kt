package xyz.xenondevs.nova.serialization.cbf

import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.cbf.io.ByteWriter
import xyz.xenondevs.cbf.serializer.UnversionedBinarySerializer
import java.awt.Color
import org.bukkit.Color as BukkitColor

internal object ColorBinarySerializer : UnversionedBinarySerializer<Color>() {
    
    override fun readUnversioned(reader: ByteReader): Color {
        return Color(reader.readInt(), true)
    }
    
    override fun writeUnversioned(obj: Color, writer: ByteWriter) {
        writer.writeInt(obj.rgb)
    }
    
    override fun copyNonNull(obj: Color): Color {
        return Color(obj.rgb, true)
    }
    
}

internal object BukkitColorBinarySerializer : UnversionedBinarySerializer<BukkitColor>() {
    
    override fun readUnversioned(reader: ByteReader): BukkitColor {
        return BukkitColor.fromARGB(reader.readInt())
    }
    
    override fun writeUnversioned(obj: BukkitColor, writer: ByteWriter) {
        writer.writeInt(obj.asARGB())
    }
    
    override fun copyNonNull(obj: BukkitColor): BukkitColor {
        return BukkitColor.fromARGB(obj.asARGB())
    }
    
}