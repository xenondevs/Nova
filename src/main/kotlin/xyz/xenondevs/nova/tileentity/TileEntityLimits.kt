package xyz.xenondevs.nova.tileentity

import org.bukkit.World
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.config.NovaConfig
import xyz.xenondevs.nova.config.PermanentStorage
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.util.GSON
import xyz.xenondevs.nova.util.PermissionUtils
import xyz.xenondevs.nova.util.enumMapOf
import xyz.xenondevs.nova.util.fromJson
import java.util.*
import kotlin.math.max

object TileEntityLimits {
    
    private val WORLD_BLACKLIST: Set<World> =
        GSON.fromJson<HashSet<World>>(NovaConfig.getArray("tile_entity_world_blacklist"))!!
    
    private val TYPE_WORLD_BLACKLIST: Map<NovaMaterial, Set<World>> =
        GSON.fromJson<EnumMap<NovaMaterial, HashSet<World>>>(NovaConfig.getObject("tile_entity_type_world_blacklist"))!!
    
    private val TYPE_AMOUNT_LIMIT: Map<NovaMaterial, Int> =
        GSON.fromJson<EnumMap<NovaMaterial, Int>>(NovaConfig.getObject("tile_entity_limit"))!!
    
    private val placedTileEntities: MutableMap<UUID, MutableMap<NovaMaterial, Int>>
    
    init {
        placedTileEntities = PermanentStorage.retrieve("placedTileEntities") { HashMap() }
        NOVA.disableHandlers += { PermanentStorage.store("placedTileEntities", placedTileEntities) }
    }
    
    fun canPlaceTileEntity(uuid: UUID, world: World, type: NovaMaterial): PlaceResult {
        if (PermissionUtils.hasPermission(world, uuid, "nova.bypassTileEntityLimits")) return PlaceResult.ALLOW
        
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
        val materialMap = placedTileEntities.getOrPut(uuid) { enumMapOf() }
        materialMap[type] = (materialMap[type] ?: 0) + 1
    }
    
    fun handleTileEntityRemove(uuid: UUID, type: NovaMaterial) {
        val materialMap = placedTileEntities.getOrPut(uuid) { enumMapOf() }
        materialMap[type] = max(0, (materialMap[type] ?: 0) - 1)
    }
    
}

enum class PlaceResult {
    
    ALLOW,
    DENY_BLACKLIST,
    DENY_BLACKLIST_TYPE,
    DENY_LIMIT
    
}