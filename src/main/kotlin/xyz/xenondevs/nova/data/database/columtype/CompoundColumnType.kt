package xyz.xenondevs.nova.data.database.columtype

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import xyz.xenondevs.nova.data.serialization.cbf.element.CompoundDeserializer
import xyz.xenondevs.nova.data.serialization.cbf.element.CompoundElement
import xyz.xenondevs.nova.util.data.compress
import xyz.xenondevs.nova.util.data.decompress

fun Table.compound(name: String): Column<CompoundElement> = registerColumn(name, CompoundColumnType())

class CompoundColumnType : MediumBlobColumnType() {
    
    override fun valueFromDB(value: Any) =
        CompoundDeserializer.read((super.valueFromDB(value) as ExposedBlob).bytes.decompress())
    
    override fun valueToDB(value: Any?) =
        super.valueToDB((value as CompoundElement).toByteArray().compress())
}