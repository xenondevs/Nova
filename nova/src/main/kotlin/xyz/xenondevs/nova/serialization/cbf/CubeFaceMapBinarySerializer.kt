package xyz.xenondevs.nova.serialization.cbf

import org.bukkit.block.BlockFace
import xyz.xenondevs.cbf.Cbf
import xyz.xenondevs.cbf.UncheckedApi
import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.cbf.io.ByteWriter
import xyz.xenondevs.cbf.serializer.BinarySerializer
import xyz.xenondevs.cbf.serializer.BinarySerializerFactory
import xyz.xenondevs.cbf.serializer.VersionedBinarySerializer
import xyz.xenondevs.commons.collections.enumMap
import xyz.xenondevs.commons.reflection.classifierClass
import xyz.xenondevs.nova.util.CubeFaceMap
import kotlin.reflect.KType

internal class CubeFaceMapBinarySerializer<V : Any>(
    private val valueSerializer: BinarySerializer<V>
) : VersionedBinarySerializer<CubeFaceMap<V?>>(2.toUByte()) {
    
    override fun readVersioned(version: UByte, reader: ByteReader): CubeFaceMap<V?> {
        when (version) {
            1.toUByte() -> return readV1(reader)
            2.toUByte() -> return readV2(reader)
            else -> throw IllegalArgumentException("Unsupported version: $version")
        }
    }
    
    private fun readV1(reader: ByteReader): CubeFaceMap<V?> {
        val size = reader.readVarInt()
        val map = enumMap<BlockFace, V>()
        repeat(size) { map[BLOCK_FACE_SERIALIZER.read(reader)] = valueSerializer.read(reader) }
        return CubeFaceMap(
            north = map[BlockFace.NORTH],
            east = map[BlockFace.EAST],
            south = map[BlockFace.SOUTH],
            west = map[BlockFace.WEST],
            up = map[BlockFace.UP],
            down = map[BlockFace.DOWN]
        )
    }
    
    
    private fun readV2(reader: ByteReader): CubeFaceMap<V?> =
        CubeFaceMap(
            north = valueSerializer.read(reader),
            east = valueSerializer.read(reader),
            south = valueSerializer.read(reader),
            west = valueSerializer.read(reader),
            up = valueSerializer.read(reader),
            down = valueSerializer.read(reader)
        )
    
    override fun writeVersioned(obj: CubeFaceMap<V?>, writer: ByteWriter) {
        valueSerializer.write(obj.north, writer)
        valueSerializer.write(obj.east, writer)
        valueSerializer.write(obj.south, writer)
        valueSerializer.write(obj.west, writer)
        valueSerializer.write(obj.up, writer)
        valueSerializer.write(obj.down, writer)
    }
    
    override fun copyNonNull(obj: CubeFaceMap<V?>): CubeFaceMap<V?> =
        CubeFaceMap(
            north = valueSerializer.copy(obj.north),
            east = valueSerializer.copy(obj.east),
            south = valueSerializer.copy(obj.south),
            west = valueSerializer.copy(obj.west),
            up = valueSerializer.copy(obj.up),
            down = valueSerializer.copy(obj.down)
        )
    
    companion object : BinarySerializerFactory {
        
        private val BLOCK_FACE_SERIALIZER by lazy { Cbf.getSerializer<BlockFace>() }
        
        @OptIn(UncheckedApi::class)
        override fun create(type: KType): BinarySerializer<*>? {
            if (type.classifierClass != CubeFaceMap::class)
                return null
            
            val valueType = type.arguments.getOrNull(0)?.type
                ?: return null
            
            return CubeFaceMapBinarySerializer(Cbf.getSerializer(valueType))
        }
        
    }
    
}

