package xyz.xenondevs.nova.world.block.limits

import org.bukkit.configuration.ConfigurationSection
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.util.item.novaMaterial
import xyz.xenondevs.nova.world.block.context.BlockPlaceContext
import xyz.xenondevs.nova.world.block.limits.BlockLimiter.Companion.ALLOWED
import xyz.xenondevs.nova.world.block.limits.TileEntityLimits.PlaceResult

internal interface BlockLimiter {
    
    fun canPlace(ctx: BlockPlaceContext): PlaceResult
    
    companion object {
    
        val ALLOWED = PlaceResult(true, "")
    
        private val limiterTypes: Map<String, (ConfigurationSection) -> BlockLimiter> = hashMapOf(
            "world" to ::WorldBlacklist,
            "type_world" to ::TypeWorldBlacklist,
            "amount" to { AmountLimiter(it, AmountLimiter.Type.GLOBAL) },
            "amount_per_world" to { AmountLimiter(it, AmountLimiter.Type.PER_WORLD) },
            "amount_per_chunk" to { AmountLimiter(it, AmountLimiter.Type.PER_CHUNK) }
        )
        
        fun createNew(type: String, cfg: ConfigurationSection): BlockLimiter? =
            limiterTypes[type]?.invoke(cfg)
        
    }
    
}

internal abstract class SimpleBlockLimiter(denyMessage: String) : BlockLimiter {
    
    private val denied = PlaceResult(false, denyMessage)
    
    final override fun canPlace(ctx: BlockPlaceContext): PlaceResult {
        return if (testPlace(ctx)) ALLOWED else denied
    }
    
    abstract fun testPlace(ctx: BlockPlaceContext): Boolean
    
}


internal class WorldBlacklist(cfg: ConfigurationSection) : SimpleBlockLimiter("nova.tile_entity_limits.world_blacklist.deny") {
    
    private val blacklist = cfg.getStringList("worlds").toHashSet()
    
    override fun testPlace(ctx: BlockPlaceContext): Boolean {
        return ctx.pos.world.name !in blacklist
    }
    
}

internal class TypeWorldBlacklist(cfg: ConfigurationSection) : SimpleBlockLimiter("nova.tile_entity_limits.world_type_blacklist.deny") {
    
    private val blacklist: Map<String, Set<NamespacedId>> =
        cfg.getKeys(false).associateWithTo(HashMap()) { world -> cfg.getStringList(world).mapTo(HashSet(), NamespacedId::of) }
    
    override fun testPlace(ctx: BlockPlaceContext): Boolean {
        val id = ctx.item.novaMaterial?.id ?: return true
        return blacklist[ctx.pos.world.name]?.contains(id) != true
    }
    
}

internal class AmountLimiter(cfg: ConfigurationSection, private val type: Type) : BlockLimiter {
    
    private val limits: Map<NamespacedId?, Int> =
        cfg.getKeys(false).associateTo(HashMap()) { id -> (if (id == "*") null else NamespacedId.of(id)) to cfg.getInt(id) }
    
    private val deniedSpecific = PlaceResult(false, "nova.tile_entity_limits.amount_${type.name.lowercase()}.deny")
    private val deniedTotal = PlaceResult(false, "nova.tile_entity_limits.amount_${type.name.lowercase()}_total.deny")
    
    override fun canPlace(ctx: BlockPlaceContext): PlaceResult {
        val id = ctx.item.novaMaterial?.id ?: return ALLOWED
        
        val specificLimit = limits[id]
        val totalLimit = limits[null]
        
        if (specificLimit != null) {
            val amount = when (type) {
                Type.GLOBAL -> TileEntityTracker.getBlocksPlacedAmount(ctx.ownerUUID, id)
                Type.PER_WORLD -> TileEntityTracker.getBlocksPlacedAmount(ctx.ownerUUID, ctx.pos.world.uid, id)
                Type.PER_CHUNK -> TileEntityTracker.getBlocksPlacedAmount(ctx.ownerUUID, ctx.pos.chunkPos, id)
            }
            
            if (amount >= specificLimit)
                return deniedSpecific
        }
        
        if (totalLimit != null) {
            val amount = when (type) {
                Type.GLOBAL -> TileEntityTracker.getBlocksPlacedAmount(ctx.ownerUUID)
                Type.PER_WORLD -> TileEntityTracker.getBlocksPlacedAmount(ctx.ownerUUID, ctx.pos.world.uid)
                Type.PER_CHUNK -> TileEntityTracker.getBlocksPlacedAmount(ctx.ownerUUID, ctx.pos.chunkPos)
            }
            
            if (amount >= totalLimit)
                return deniedTotal
        }
        
        return ALLOWED
    }
    
    enum class Type {
        GLOBAL,
        PER_WORLD,
        PER_CHUNK
    }
    
}