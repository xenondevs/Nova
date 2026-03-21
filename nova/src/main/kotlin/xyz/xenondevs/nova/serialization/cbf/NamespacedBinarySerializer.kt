package xyz.xenondevs.nova.serialization.cbf

import net.minecraft.resources.Identifier
import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.cbf.io.ByteWriter
import xyz.xenondevs.cbf.serializer.UnversionedBinarySerializer

internal object IdentifierBinarySerializer : UnversionedBinarySerializer<Identifier>() {
    
    override fun readUnversioned(reader: ByteReader): Identifier {
        return Identifier.parse(reader.readString())
    }
    
    override fun writeUnversioned(obj: Identifier, writer: ByteWriter) {
        writer.writeString(obj.toString())
    }
    
    override fun copyNonNull(obj: Identifier): Identifier {
        return obj
    }
    
}