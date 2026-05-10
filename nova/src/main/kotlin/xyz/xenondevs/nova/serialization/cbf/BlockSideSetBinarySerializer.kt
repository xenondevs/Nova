package xyz.xenondevs.nova.serialization.cbf

import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.cbf.io.ByteWriter
import xyz.xenondevs.cbf.serializer.UnversionedBinarySerializer
import xyz.xenondevs.nova.util.BlockSideSet

internal object BlockSideSetBinarySerializer : UnversionedBinarySerializer<BlockSideSet>() {

    override fun readUnversioned(reader: ByteReader) = BlockSideSet(reader.readByte())

    override fun writeUnversioned(obj: BlockSideSet, writer: ByteWriter) {
        writer.writeByte(obj.data)
    }

    override fun copyNonNull(obj: BlockSideSet) = obj

}
