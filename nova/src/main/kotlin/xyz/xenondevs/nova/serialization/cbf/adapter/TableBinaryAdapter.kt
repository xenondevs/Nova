package xyz.xenondevs.nova.serialization.cbf.adapter

import com.google.common.collect.HashBasedTable
import com.google.common.collect.Table
import xyz.xenondevs.cbf.CBF
import xyz.xenondevs.cbf.adapter.BinaryAdapter
import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.cbf.io.ByteWriter
import xyz.xenondevs.commons.reflection.nonNullTypeArguments
import kotlin.reflect.KType

internal object TableBinaryAdapter : BinaryAdapter<Table<*, *, *>> {
    
    override fun copy(obj: Table<*, *, *>, type: KType): Table<*, *, *> {
        val (rowType, columnType, valueType) = type.nonNullTypeArguments
        val table = createTableInstance(type)
        for (cell in obj.cellSet()) {
            table.put(
                cell.rowKey?.let { CBF.copy(it, rowType) },
                cell.columnKey?.let { CBF.copy(it, columnType) },
                cell.value?.let { CBF.copy(it, valueType) }
            )
        }
        
        return table
    }
    
    override fun read(type: KType, reader: ByteReader): Table<*, *, *> {
        val (rowType, columnType, valueType) = type.nonNullTypeArguments
        val table = createTableInstance(type)
        val size = reader.readVarInt()
        repeat(size) {
            table.put(
                CBF.read(rowType, reader),
                CBF.read(columnType, reader),
                CBF.read(valueType, reader)
            )
        }
        
        return table
    }
    
    override fun write(obj: Table<*, *, *>, type: KType, writer: ByteWriter) {
        val (rowType, columnType, valueType) = type.nonNullTypeArguments
        writer.writeVarInt(obj.size())
        obj.cellSet().forEach { cell ->
            CBF.write(cell.rowKey, rowType, writer)
            CBF.write(cell.columnKey, columnType, writer)
            CBF.write(cell.value, valueType, writer)
        }
    }
    
    private fun createTableInstance(type: KType): Table<Any?, Any?, Any?> =
        CBF.createInstance(type) ?: HashBasedTable.create<Any?, Any?, Any?>()
    
}