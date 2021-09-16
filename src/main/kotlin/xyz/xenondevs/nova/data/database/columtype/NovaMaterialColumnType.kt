package xyz.xenondevs.nova.data.database.columtype

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.IColumnType
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.VarCharColumnType
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.material.NovaMaterialRegistry

fun Table.novaMaterial(name: String): Column<NovaMaterial> = registerColumn(name, NovaMaterialColumnType())

class NovaMaterialColumnType : IColumnType by VarCharColumnType(128) {
    
    override fun valueFromDB(value: Any) =
        NovaMaterialRegistry.get(super.valueFromDB(value) as String)
    
    override fun valueToDB(value: Any?) =
        super.valueToDB((value as NovaMaterial).typeName)
    
}