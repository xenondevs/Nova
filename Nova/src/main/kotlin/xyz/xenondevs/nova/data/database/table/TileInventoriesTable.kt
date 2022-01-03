package xyz.xenondevs.nova.data.database.table

import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.ReferenceOption.CASCADE
import xyz.xenondevs.nova.data.database.columtype.virtualInventory
import java.util.*

object TileInventoriesTable : IdTable<UUID>() {
    
    override val id = uuid("id").entityId()
    val tileEntityId = reference(
        "tileEntityId",
        TileEntitiesTable,
        onDelete = CASCADE,
        onUpdate = CASCADE,
        fkName = "FK_inventory_tile_entity")
    val data = virtualInventory("data")
    
    override val primaryKey = PrimaryKey(id)
    
}