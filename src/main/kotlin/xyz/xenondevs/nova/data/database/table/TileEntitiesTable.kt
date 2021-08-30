package xyz.xenondevs.nova.data.database.table

import org.jetbrains.exposed.sql.Table
import xyz.xenondevs.nova.data.database.columtype.mediumBlob

object TileEntitiesTable : Table() {
    
    val uuid = uuid("uuid")
    val world = uuid("world")
    val owner = uuid("owner")
    val chunkX = integer("chunkX")
    val chunkZ = integer("chunkZ")
    val x = integer("x")
    val y = integer("y")
    val z = integer("z")
    val yaw = float("yaw")
    val type = varchar("type", 128)
    val data = mediumBlob("data")
    
    override val primaryKey = PrimaryKey(uuid)
    
}
