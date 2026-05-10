package xyz.xenondevs.nova.serialization.cbf

import xyz.xenondevs.cbf.Cbf
import xyz.xenondevs.cbf.UncheckedApi
import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.cbf.io.ByteWriter
import xyz.xenondevs.cbf.serializer.BinarySerializer
import xyz.xenondevs.cbf.serializer.BinarySerializerFactory
import xyz.xenondevs.cbf.serializer.UnversionedBinarySerializer
import xyz.xenondevs.commons.reflection.classifierClass
import xyz.xenondevs.nova.util.BlockSideMap
import kotlin.reflect.KType

internal class BlockSideMapBinarySerializer<V : Any>(
    private val valueSerializer: BinarySerializer<V>
) : UnversionedBinarySerializer<BlockSideMap<V?>>() {

    override fun readUnversioned(reader: ByteReader): BlockSideMap<V?> =
        BlockSideMap(
            front = valueSerializer.read(reader),
            left = valueSerializer.read(reader),
            back = valueSerializer.read(reader),
            right = valueSerializer.read(reader),
            top = valueSerializer.read(reader),
            bottom = valueSerializer.read(reader)
        )

    override fun writeUnversioned(obj: BlockSideMap<V?>, writer: ByteWriter) {
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