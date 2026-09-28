package xyz.xenondevs.nova.serialization.cbf

import org.bukkit.block.BlockFace
import xyz.xenondevs.cbf.Cbf
import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.cbf.io.ByteWriter
import xyz.xenondevs.cbf.serializer.VersionedBinarySerializer
import xyz.xenondevs.commons.collections.enumSet
import xyz.xenondevs.nova.util.CubeFaceSet

internal object CubeFaceSetBinarySerializer : VersionedBinarySerializer<CubeFaceSet>(2.toUByte()) {
    
    private val BLOCK_FACE_SERIALIZER by lazy { Cbf.getSerializer<BlockFace>() }
    
    override fun readVersioned(version: UByte, reader: ByteReader): CubeFaceSet {
        return when (version) {
            1.toUByte() -> readV1(reader)
            2.toUByte() -> readV2(reader)
            else -> error("Unsupported version: $version")
        }
    }
    
    private fun readV1(reader: ByteReader): CubeFaceSet {
        val size = reader.readVarInt()
        val set = enumSet<BlockFace>()
        repeat(size) { set += BLOCK_FACE_SERIALIZER.read(reader) }
        return CubeFaceSet(
            north = BlockFace.NORTH in set,
            east = BlockFace.EAST in set,
            south = BlockFace.SOUTH in set,
            west = BlockFace.WEST in set,
            up = BlockFace.UP in set,
            down = BlockFace.DOWN in set
        )
    }
    
    private fun readV2(reader: ByteReader) = CubeFaceSet(reader.readByte())

    override fun writeVersioned(obj: CubeFaceSet, writer: ByteWriter) {
        writer.writeByte(obj.data)
    }

    override fun copyNonNull(obj: CubeFaceSet) = obj

}