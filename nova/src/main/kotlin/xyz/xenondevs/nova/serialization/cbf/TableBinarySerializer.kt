package xyz.xenondevs.nova.serialization.cbf

import com.google.common.collect.HashBasedTable
import com.google.common.collect.Table
import com.google.common.collect.TreeBasedTable
import xyz.xenondevs.cbf.Cbf
import xyz.xenondevs.cbf.UncheckedApi
import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.cbf.io.ByteWriter
import xyz.xenondevs.cbf.io.byteWriter
import xyz.xenondevs.cbf.serializer.BinarySerializer
import xyz.xenondevs.cbf.serializer.BinarySerializerFactory
import xyz.xenondevs.cbf.serializer.UnversionedBinarySerializer
import xyz.xenondevs.commons.guava.component1
import xyz.xenondevs.commons.guava.component2
import xyz.xenondevs.commons.guava.component3
import xyz.xenondevs.commons.guava.set
import xyz.xenondevs.commons.reflection.classifierClass
import xyz.xenondevs.commons.reflection.isSubtypeOf
import kotlin.reflect.KType

private typealias TableCreator<R, C, V> = () -> Table<R, C, V>

internal class TableBinarySerializer<R : Any, C : Any, V : Any>(
    private val rowKeySerializer: BinarySerializer<R>,
    private val columnKeySerializer: BinarySerializer<C>,
    private val valueSerializer: BinarySerializer<V>,
    private val createTable: TableCreator<R?, C?, V?>
) : UnversionedBinarySerializer<Table<R?, C?, V?>>() {
    
    override fun readUnversioned(reader: ByteReader): Table<R?, C?, V?> {
        val size = reader.readVarInt()
        val table = createTable()
        
        repeat(size) {
            val rowKey = rowKeySerializer.read(reader)
            val columnKey = columnKeySerializer.read(reader)
            val value = valueSerializer.read(reader)
            table[rowKey, columnKey] = value
        }
        
        return table
    }
    
    override fun writeUnversioned(obj: Table<R?, C?, V?>, writer: ByteWriter) {
        // count serialized elements instead of obj.size to prevent concurrent modifications from causing corrupted data
        var size = 0
        val temp = byteWriter {
            for ((rowKey, columnKey, value) in obj.cellSet()) {
                rowKeySerializer.write(rowKey, this)
                columnKeySerializer.write(columnKey, this)
                valueSerializer.write(value, this)
                size++
            }
        }
        
        writer.writeVarInt(size)
        writer.writeBytes(temp)
    }
    
    override fun copyNonNull(obj: Table<R?, C?, V?>): Table<R?, C?, V?> {
        val copy = createTable()
        for ((rowKey, columnKey, value) in obj.cellSet()) {
            copy[rowKeySerializer.copy(rowKey), columnKeySerializer.copy(columnKey)] = valueSerializer.copy(value)
        }
        return copy
    }
    
    companion object : BinarySerializerFactory {
        
        @OptIn(UncheckedApi::class)
        override fun create(type: KType): BinarySerializer<*>? {
            if (!type.isSubtypeOf<Table<*, *, *>?>())
                return null
            
            val rowKeyType = type.arguments.getOrNull(0)?.type
                ?: return null
            val columnKeyType = type.arguments.getOrNull(1)?.type
                ?: return null
            val valueType = type.arguments.getOrNull(2)?.type
                ?: return null
            val tableCreator = getTableCreator(type)
                ?: return null
            
            return TableBinarySerializer(
                Cbf.getSerializer(rowKeyType),
                Cbf.getSerializer(columnKeyType),
                Cbf.getSerializer(valueType),
                tableCreator
            )
        }
        
        @Suppress("UNCHECKED_CAST")
        private fun getTableCreator(type: KType): TableCreator<Any?, Any?, Any?>? {
            TreeBasedTable.create<Comparable<*>, Comparable<*>, Any>()
            return when(type.classifierClass) {
                HashBasedTable::class -> { { HashBasedTable.create<Any, Any, Any>() } }
                TreeBasedTable::class -> { { TreeBasedTable.create<Comparable<*>, Comparable<*>, Any>() } as TableCreator<Any, Any, Any> }
                Table::class -> { { HashBasedTable.create<Any, Any, Any>() } }
                else -> null
            } as TableCreator<Any?, Any?, Any?>?
        }
        
    }
    
}