package xyz.xenondevs.nova.database.table

import org.jetbrains.exposed.sql.Table

object ModelsTable : Table("models") {
    
    val uuid = uuid("uuid")
    val world = uuid("world")
    val x = integer("x")
    val y = integer("y")
    val z = integer("z")
    val data = text("data") // TODO: binary
    
    override val primaryKey = PrimaryKey(uuid)
    
}