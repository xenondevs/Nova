package xyz.xenondevs.nova.database.table

import org.jetbrains.exposed.sql.Table

object TileEntitiesTable : Table("tileEntities") {
    
    val uuid = uuid("uuid")
    val world = uuid("world")
    val chunkX = integer("chunkX")
    val chunkZ = integer("chunkZ")
    val x = integer("x")
    val y = integer("y")
    val z = integer("z")
    val yaw = float("yaw")
    val type = varchar("type", 128)
    val data = blob("data")
    
    override val primaryKey = PrimaryKey(uuid)
    
}
