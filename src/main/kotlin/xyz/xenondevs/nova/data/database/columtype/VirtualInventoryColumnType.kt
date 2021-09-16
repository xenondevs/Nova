package xyz.xenondevs.nova.data.database.columtype

import de.studiocode.invui.virtualinventory.VirtualInventory
import de.studiocode.invui.virtualinventory.VirtualInventoryManager
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

fun Table.virtualInventory(name: String): Column<VirtualInventory> = registerColumn(name, VirtualInventoryColumnType())

class VirtualInventoryColumnType : MediumBlobColumnType() {
    
    override fun valueFromDB(value: Any): Any {
        return VirtualInventoryManager.getInstance()
            .deserializeInventory(ByteArrayInputStream((super.valueFromDB(value) as ExposedBlob).bytes))
    }
    
    override fun valueToDB(value: Any?): Any? {
        val out = ByteArrayOutputStream()
        VirtualInventoryManager.getInstance().serializeInventory(value as VirtualInventory, out)
        return super.valueToDB(out.toByteArray())
    }
    
}