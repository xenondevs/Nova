package xyz.xenondevs.nova.data.database.table

import org.jetbrains.exposed.sql.ReferenceOption.CASCADE
import org.jetbrains.exposed.sql.Table
import xyz.xenondevs.nova.data.database.columtype.mediumBlob

object TileEntityInventoriesTable : Table() {
    
    val uuid = uuid("uuid")
    val tileEntityId = uuid("tileEntityId").references(
        TileEntitiesTable.uuid,
        onDelete = CASCADE,
        onUpdate = CASCADE,
        fkName = "FK_inventory_tile_entity"
    )
    val data = mediumBlob("data")
    
}