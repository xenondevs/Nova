package xyz.xenondevs.nova.tileentity

import org.bukkit.World
import xyz.xenondevs.nova.data.config.DEFAULT_CONFIG
import xyz.xenondevs.nova.material.ItemNovaMaterial
import xyz.xenondevs.nova.util.PermissionUtils
import xyz.xenondevs.nova.util.data.GSON
import xyz.xenondevs.nova.util.data.fromJson
import java.util.*
import kotlin.math.max

object TileEntityLimits {
    
    private val WORLD_BLACKLIST: Set<World> =
        GSON.fromJson<HashSet<World>>(DEFAULT_CONFIG.getArray("tile_entity_world_blacklist"))!!
    
    private val TYPE_WORLD_BLACKLIST: Map<ItemNovaMaterial, Set<World>> =
        GSON.fromJson<HashMap<ItemNovaMaterial, HashSet<World>>>(DEFAULT_CONFIG.getObject("tile_entity_type_world_blacklist"))!!
    
    private val TYPE_AMOUNT_LIMIT: Map<ItemNovaMaterial, Int> =
        GSON.fromJson<HashMap<ItemNovaMaterial, Int>>(DEFAULT_CONFIG.getObject("tile_entity_limit"))!!
    
    private val placedTileEntities = HashMap<UUID, MutableMap<ItemNovaMaterial, Int>>()
    
    init {
        // TODO
    }
    
    fun canPlaceTileEntity(uuid: UUID, world: World, type: ItemNovaMaterial): PlaceResult {
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
    
    fun handleTileEntityCreate(uuid: UUID, type: ItemNovaMaterial) {
        val materialMap = placedTileEntities.getOrPut(uuid) { hashMapOf() }
        materialMap[type] = (materialMap[type] ?: 0) + 1
    }
    
    fun handleTileEntityRemove(uuid: UUID, type: ItemNovaMaterial) {
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