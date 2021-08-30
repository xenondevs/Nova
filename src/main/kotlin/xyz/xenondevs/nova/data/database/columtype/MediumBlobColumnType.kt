package xyz.xenondevs.nova.data.database.columtype

import org.jetbrains.exposed.sql.BlobColumnType
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.IColumnType
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.statements.api.ExposedBlob

fun Table.mediumBlob(name: String): Column<ExposedBlob> = registerColumn(name, MediumBlobColumnType())

class MediumBlobColumnType : IColumnType by BlobColumnType() {
    override fun sqlType() = "MEDIUMBLOB"
}