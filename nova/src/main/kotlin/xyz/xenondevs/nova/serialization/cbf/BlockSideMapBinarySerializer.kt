package xyz.xenondevs.nova.serialization.cbf

import xyz.xenondevs.cbf.Cbf
import xyz.xenondevs.cbf.UncheckedApi
import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.cbf.io.ByteWriter
import xyz.xenondevs.cbf.serializer.BinarySerializer
import xyz.xenondevs.cbf.serializer.BinarySerializerFactory
import xyz.xenondevs.cbf.serializer.VersionedBinarySerializer
import xyz.xenondevs.commons.collections.enumMap
import xyz.xenondevs.commons.reflection.classifierClass
import xyz.xenondevs.nova.util.BlockSide
import xyz.xenondevs.nova.util.BlockSideMap
import kotlin.reflect.KType

internal class BlockSideMapBinarySerializer<V : Any>(
    private val valueSerializer: BinarySerializer<V>
) : VersionedBinarySerializer<BlockSideMap<V?>>(2.toUByte()) {
    
    override fun readVersioned(version: UByte, reader: ByteReader): BlockSideMap<V?> {
        when (version) {
            1.toUByte() -> return readV1(reader)
            2.toUByte() -> return readV2(reader)
            else -> throw IllegalArgumentException("Unsupported version: $version")
        }
    }
    
    private fun readV1(reader: ByteReader): BlockSideMap<V?> {
        val size = reader.readVarInt()
        val map = enumMap<BlockSide, V>()
        repeat(size) { map[BLOCK_SIDE_SERIALIZER.read(reader)] = valueSerializer.read(reader) }
        return BlockSideMap(
            front = map[BlockSide.FRONT],
            left = map[BlockSide.LEFT],
            back = map[BlockSide.BACK],
            right = map[BlockSide.RIGHT],
            top = map[BlockSide.TOP],
            bottom = map[BlockSide.BOTTOM]
        )
    }
    
    private fun readV2(reader: ByteReader): BlockSideMap<V?> =
        BlockSideMap(
            front = valueSerializer.read(reader),
            left = valueSerializer.read(reader),
            back = valueSerializer.read(reader),
            right = valueSerializer.read(reader),
            top = valueSerializer.read(reader),
            bottom = valueSerializer.read(reader)
        )
    
    override fun writeVersioned(obj: BlockSideMap<V?>, writer: ByteWriter) {
        valueSerializer.write(obj.front, writer)
        valueSerializer.write(obj.left, writer)
        valueSerializer.write(obj.back, writer)
        valueSerializer.write(obj.right, writer)
        valueSerializer.write(obj.top, writer)
        valueSerializer.write(obj.bottom, writer)
    }
    
    override fun copyNonNull(obj: BlockSideMap<V?>): BlockSideMap<V?> =
        BlockSideMap(
            front = valueSerializer.copy(obj.front),
            left = valueSerializer.copy(obj.left),
            back = valueSerializer.copy(obj.back),
            right = valueSerializer.copy(obj.right),
            top = valueSerializer.copy(obj.top),
            bottom = valueSerializer.copy(obj.bottom)
        )
    
    companion object : BinarySerializerFactory {
        
        private val BLOCK_SIDE_SERIALIZER by lazy { Cbf.getSerializer<BlockSide>() }
        
        @OptIn(UncheckedApi::class)
        override fun create(type: KType): BinarySerializer<*>? {
            if (type.classifierClass != BlockSideMap::class)
                return null
            
            val valueType = type.arguments.getOrNull(0)?.type
                ?: return null
            
            return BlockSideMapBinarySerializer(Cbf.getSerializer(valueType))
        }
        
    }
    
}