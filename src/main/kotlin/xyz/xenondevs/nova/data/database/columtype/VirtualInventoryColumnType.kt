package xyz.xenondevs.nova.data.database.columtype

import de.studiocode.invui.virtualinventory.VirtualInventory
import de.studiocode.invui.virtualinventory.VirtualInventoryManager
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.statements.api.ExposedBlob

fun Table.virtualInventory(name: String): Column<VirtualInventory> = registerColumn(name, VirtualInventoryColumnType())

class VirtualInventoryColumnType : MediumBlobColumnType() {
    
    override fun valueFromDB(value: Any): VirtualInventory =
        VirtualInventoryManager.getInstance().deserializeInventory(((super.valueFromDB(value) as ExposedBlob).bytes))
    
    override fun valueToDB(value: Any?): ByteArray =
        (value as VirtualInventory).toByteArray()
    
}