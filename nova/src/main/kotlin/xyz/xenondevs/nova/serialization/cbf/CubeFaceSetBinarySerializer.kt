package xyz.xenondevs.nova.serialization.cbf

import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.cbf.io.ByteWriter
import xyz.xenondevs.cbf.serializer.UnversionedBinarySerializer
import xyz.xenondevs.nova.util.CubeFaceSet

internal object CubeFaceSetBinarySerializer : UnversionedBinarySerializer<CubeFaceSet>() {

    override fun readUnversioned(reader: ByteReader) = CubeFaceSet(reader.readByte())

    override fun writeUnversioned(obj: CubeFaceSet, writer: ByteWriter) {
        writer.writeByte(obj.data)
    }

    override fun copyNonNull(obj: CubeFaceSet) = obj

}