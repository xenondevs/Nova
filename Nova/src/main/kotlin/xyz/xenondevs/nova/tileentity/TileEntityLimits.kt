package xyz.xenondevs.nova.tileentity

import org.bukkit.World
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import xyz.xenondevs.nova.data.config.DEFAULT_CONFIG
import xyz.xenondevs.nova.data.database.table.TileEntitiesTable
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.util.PermissionUtils
import xyz.xenondevs.nova.util.data.GSON
import xyz.xenondevs.nova.util.data.fromJson
import java.util.*
import kotlin.math.max

object TileEntityLimits {
    
    private val WORLD_BLACKLIST: Set<World> =
        GSON.fromJson<HashSet<World>>(DEFAULT_CONFIG.getArray("tile_entity_world_blacklist"))!!
    
    private val TYPE_WORLD_BLACKLIST: Map<NovaMaterial, Set<World>> =
        GSON.fromJson<HashMap<NovaMaterial, HashSet<World>>>(DEFAULT_CONFIG.getObject("tile_entity_type_world_blacklist"))!!
    
    private val TYPE_AMOUNT_LIMIT: Map<NovaMaterial, Int> =
        GSON.fromJson<HashMap<NovaMaterial, Int>>(DEFAULT_CONFIG.getObject("tile_entity_limit"))!!
    
    private val placedTileEntities = HashMap<UUID, MutableMap<NovaMaterial, Int>>()
    
    init {
        transaction {
            val countExpr = TileEntitiesTable.owner.count()
            TileEntitiesTable
                .slice(TileEntitiesTable.owner, TileEntitiesTable.type, countExpr)
                .selectAll()
                .groupBy(TileEntitiesTable.owner, TileEntitiesTable.type)
                .forEach { row ->
                    val owner = row[TileEntitiesTable.owner]
                    val type = row[TileEntitiesTable.type]
                    val count = row[countExpr].toInt()
                    if (owner !in placedTileEntities)
                        placedTileEntities[owner] = HashMap()
                    placedTileEntities[owner]!![type] = count
                }
        }
    }
    
    fun canPlaceTileEntity(uuid: UUID, world: World, type: NovaMaterial): PlaceResult {
        if (PermissionUtils.hasPermission(world, uuid, "nova.misc.bypassTileEntityLimits")) return PlaceResult.ALLOW
        
        if (WORLD_BLACKLIST.contains(world)) return PlaceResult.DENY_BLACKLIST
        if (TYPE_WORLD_BLACKLIST.containsKey(type) && TYPE_WORLD_BLACKLIST[type]?.contains(world) == true) return PlaceResult.DENY_BLACKLIST_TYPE
        
        val limit = TYPE_AMOUNT_LIMIT[type]
        if (limit != null && limit >= 0) {
            val placedAmount = placedTileEntities[uuid]?.get(type) ?: 0
            if (placedAmount >= limit) return PlaceResult.DENY_LIMIT
        }
        
        return PlaceResult.ALLOW
    }
    
    fun handleTileEntityCreate(uuid: UUID, type: NovaMaterial) {
        val materialMap = placedTileEntities.getOrPut(uuid) { hashMapOf() }
        materialMap[type] = (materialMap[type] ?: 0) + 1
    }
    
    fun handleTileEntityRemove(uuid: UUID, type: NovaMaterial) {
        val materialMap = placedTileEntities.getOrPut(uuid) { hashMapOf() }
        materialMap[type] = max(0, (materialMap[type] ?: 0) - 1)
    }
    
}

enum class PlaceResult {
    
    ALLOW,
    DENY_BLACKLIST,
    DENY_BLACKLIST_TYPE,
    DENY_LIMIT
    
}