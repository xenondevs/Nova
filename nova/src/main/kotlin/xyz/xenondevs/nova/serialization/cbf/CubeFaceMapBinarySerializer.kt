package xyz.xenondevs.nova.serialization.cbf

import xyz.xenondevs.cbf.Cbf
import xyz.xenondevs.cbf.UncheckedApi
import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.cbf.io.ByteWriter
import xyz.xenondevs.cbf.serializer.BinarySerializer
import xyz.xenondevs.cbf.serializer.BinarySerializerFactory
import xyz.xenondevs.cbf.serializer.UnversionedBinarySerializer
import xyz.xenondevs.commons.reflection.classifierClass
import xyz.xenondevs.nova.util.CubeFaceMap
import kotlin.reflect.KType

internal class CubeFaceMapBinarySerializer<V : Any>(
    private val valueSerializer: BinarySerializer<V>
) : UnversionedBinarySerializer<CubeFaceMap<V?>>() {

    override fun readUnversioned(reader: ByteReader): CubeFaceMap<V?> =
        CubeFaceMap(
            north = valueSerializer.read(reader),
            east = valueSerializer.read(reader),
            south = valueSerializer.read(reader),
            west = valueSerializer.read(reader),
            up = valueSerializer.read(reader),
            down = valueSerializer.read(reader)
        )

    override fun writeUnversioned(obj: CubeFaceMap<V?>, writer: ByteWriter) {
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

