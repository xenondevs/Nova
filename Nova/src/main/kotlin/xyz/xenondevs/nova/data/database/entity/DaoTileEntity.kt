package xyz.xenondevs.nova.data.database.entity

import org.bukkit.Bukkit
import org.bukkit.Location
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import xyz.xenondevs.nova.data.database.table.TileEntitiesTable
import java.util.*

class DaoTileEntity(id: EntityID<UUID>) : Entity<UUID>(id) {
    companion object : EntityClass<UUID, DaoTileEntity>(TileEntitiesTable)
    
    var world by TileEntitiesTable.world
    var owner by TileEntitiesTable.owner
    var chunkX by TileEntitiesTable.chunkX
    var chunkZ by TileEntitiesTable.chunkZ
    var x by TileEntitiesTable.x
    var y by TileEntitiesTable.y
    var z by TileEntitiesTable.z
    var yaw by TileEntitiesTable.yaw
    var type by TileEntitiesTable.type
    var data by TileEntitiesTable.data
    
    val location
        get() = Location(Bukkit.getWorld(world), x.toDouble(), y.toDouble(), z.toDouble())
}