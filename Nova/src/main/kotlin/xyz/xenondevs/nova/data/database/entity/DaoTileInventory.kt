package xyz.xenondevs.nova.data.database.entity

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import xyz.xenondevs.nova.data.database.table.TileInventoriesTable
import java.util.*

class DaoTileInventory(id: EntityID<UUID>) : Entity<UUID>(id) {
    companion object : EntityClass<UUID, DaoTileInventory>(TileInventoriesTable)
    
    var tileEntity by DaoTileEntity referencedOn TileInventoriesTable.tileEntityId
    var data by TileInventoriesTable.data
}