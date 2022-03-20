package xyz.xenondevs.nova.data.database.columtype

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.IColumnType
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.VarCharColumnType
import xyz.xenondevs.nova.material.ItemNovaMaterial
import xyz.xenondevs.nova.material.NovaMaterialRegistry

fun Table.novaMaterial(name: String): Column<ItemNovaMaterial> = registerColumn(name, NovaMaterialColumnType())

class NovaMaterialColumnType : IColumnType by VarCharColumnType(128) {
    
    override fun valueFromDB(value: Any): ItemNovaMaterial =
        NovaMaterialRegistry.get(super.valueFromDB(value) as String)
    
    override fun valueToDB(value: Any?) =
        super.valueToDB((value as ItemNovaMaterial).id)
    
}