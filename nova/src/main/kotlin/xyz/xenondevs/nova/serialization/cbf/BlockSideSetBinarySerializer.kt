package xyz.xenondevs.nova.serialization.cbf

import xyz.xenondevs.cbf.Cbf
import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.cbf.io.ByteWriter
import xyz.xenondevs.cbf.serializer.VersionedBinarySerializer
import xyz.xenondevs.commons.collections.enumSetOf
import xyz.xenondevs.nova.util.BlockSide
import xyz.xenondevs.nova.util.BlockSideSet

internal object BlockSideSetBinarySerializer : VersionedBinarySerializer<BlockSideSet>(2.toUByte()) {
    
    private val BLOCK_SIDE_SERIALIZER by lazy { Cbf.getSerializer<BlockSide>() }
    
    override fun readVersioned(version: UByte, reader: ByteReader): BlockSideSet {
        return when (version) {
            1.toUByte() -> readV1(reader)
            2.toUByte() -> readV2(reader)
            else -> error("Unsupported version: $version")
        }
    }
    
    private fun readV1(reader: ByteReader): BlockSideSet {
        val size = reader.readVarInt()
        val set = enumSetOf<BlockSide>()
        repeat(size) { set += BLOCK_SIDE_SERIALIZER.read(reader) }
        return BlockSideSet(
            front = BlockSide.FRONT in set,
            back = BlockSide.BACK in set,
            left = BlockSide.LEFT in set,
            right = BlockSide.RIGHT in set,
            top = BlockSide.TOP in set,
            bottom = BlockSide.BOTTOM in set
        )
    }
    
    private fun readV2(reader: ByteReader) = BlockSideSet(reader.readByte())
    
    override fun writeVersioned(obj: BlockSideSet, writer: ByteWriter) {
        writer.writeByte(obj.data)
    }
    
    override fun copyNonNull(obj: BlockSideSet) = obj
    
}
